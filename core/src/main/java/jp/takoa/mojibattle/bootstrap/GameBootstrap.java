package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.custom.CustomCharacterStore;
import jp.takoa.mojibattle.data.RegistryResolver;
import jp.takoa.mojibattle.data.validator.TranslationDataValidator;
import jp.takoa.mojibattle.domain.CharacterRegistry;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.support.SupportRegistry;
import jp.takoa.mojibattle.event.EventChannels;
import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.event.lifecycle.DataLoadedEvent;
import jp.takoa.mojibattle.event.lifecycle.RegistriesFrozenEvent;
import jp.takoa.mojibattle.event.lifecycle.RegistriesRegisteredEvent;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.mod.ModContext;
import jp.takoa.mojibattle.registry.MojiBattleRegistries;
import jp.takoa.mojibattle.registry.RegistryManager;
import jp.takoa.mojibattle.registry.RegistrySnapshot;
import jp.takoa.mojibattle.resource.ClasspathResourceManager;
import jp.takoa.mojibattle.resource.ResourceManager;
import jp.takoa.mojibattle.runtime.factory.AbilityRuntimeFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** ゲーム本体と全MODの登録、データ解決、Registry凍結を調停するComposition Root。 */
public final class GameBootstrap {
    private GameBootstrap() {
    }

    /** 標準クラスパスリソースだけを使う従来呼出し向け入口。 */
    public static RegistrySnapshot bootstrap(Iterable<? extends GameMod> mods) {
        return bootstrap(mods, new ClasspathResourceManager(GameBootstrap.class.getClassLoader()));
    }

    /**
     * MODを指定順に登録し、既存JSONを解決してからRegistryを凍結する。
     * libGDXの内部ファイルAPIが利用可能になった後、起動につき一度だけ呼ぶ。
     *
     * @param mods 標準MODと依存解決済み外部MOD
     * @return 画面構築と戦闘開始に必要な共有サービス
     */
    public static RegistrySnapshot bootstrap(Iterable<? extends GameMod> mods, ResourceManager resources) {
        EventChannels events = new EventChannels();
        RegistryManager manager = new RegistryManager();
        MojiBattleRegistries registries = new MojiBattleRegistries(manager);
        List<GameMod> orderedMods = validateMods(mods);
        try {
            for (GameMod mod : orderedMods) {
                mod.register(new ModContext(mod.id(), scoped(events.lifecycle(), mod.id()), resources));
            }
            manager.publishRegistrationEvents(events.lifecycle());
            events.lifecycle().post(new RegistriesRegisteredEvent(manager));

            var additionalCharacters = List.copyOf(registries.characters.values());
            var additionalSupports = List.copyOf(registries.supportCharacters.values());
            var additionalFieldEffects = List.copyOf(registries.fieldEffects.values());

            ConfigManager config = new ConfigManager(registries, resources, additionalCharacters,
                    additionalSupports, additionalFieldEffects);
            events.lifecycle().post(new DataLoadedEvent(config));
            LocalizationManager i18n = new LocalizationManager(
                    config.game.language,
                    resources,
                    java.util.stream.Stream.concat(additionalCharacters.stream()
                            .map(registration -> registration.translation(config.game.language))
                            , additionalSupports.stream()
                                    .map(registration -> registration.translation(config.game.language)))
                            .distinct()
                            .toList());
            new TranslationDataValidator().validate(config, i18n);

            CustomCharacterStore customCharacters = new CustomCharacterStore(config);
            CharacterRegistry characters = new CharacterRegistry();
            SupportRegistry supports = new SupportRegistry();
            config.characters().forEach(characters::register);
            config.supports().forEach(supports::register);

            for (var custom : customCharacters.all()) {
                if (custom.type == CharacterType.MAIN) {
                    characters.register(customCharacters.createMain(custom));
                } else {
                    supports.register(customCharacters.createSupport(custom));
                }
            }

            AbilityRegistry abilities = new AbilityRuntimeFactory(new RegistryResolver(registries)).create(config);
            manager.freezeAll();
            events.lifecycle().post(new RegistriesFrozenEvent(manager));
            for (GameMod mod : orderedMods) {
                mod.initialize(new ModContext(mod.id(), scoped(events.lifecycle(), mod.id()), resources));
            }
            return new RegistrySnapshot(registries, config, i18n, characters, supports, abilities,
                    customCharacters, events, resources);
        } catch (RuntimeException error) {
            events.close();
            throw error;
        }
    }

    private static List<GameMod> validateMods(Iterable<? extends GameMod> mods) {
        List<GameMod> ordered = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (GameMod mod : mods) {
            if (mod == null || mod.id() == null || mod.id().isBlank()) {
                throw new IllegalArgumentException("MOD and MOD ID are required");
            }
            if (!ids.add(mod.id())) {
                throw new IllegalArgumentException("Duplicate loaded MOD ID: " + mod.id());
            }
            ordered.add(mod);
        }
        return List.copyOf(ordered);
    }

    private static RegistrationBus scoped(RegistrationBus delegate, String namespace) {
        return new RegistrationBus() {
            @Override public String ownerNamespace() { return namespace; }
            @Override public <E> jp.takoa.mojibattle.event.EventSubscription subscribe(
                    Class<E> eventType, java.util.function.Consumer<? super E> listener) {
                return delegate.subscribe(eventType, listener);
            }
        };
    }
}
