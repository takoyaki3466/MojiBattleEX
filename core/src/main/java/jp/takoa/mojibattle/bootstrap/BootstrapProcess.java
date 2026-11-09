package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.content.FieldEffectDataRegistration;
import jp.takoa.mojibattle.content.SupportCharacterDataRegistration;
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
import jp.takoa.mojibattle.resource.ResourceManager;
import jp.takoa.mojibattle.runtime.factory.AbilityRuntimeFactory;

import java.util.List;
import java.util.stream.Stream;

/**
 * {@link GameBootstrap#bootstrap(Iterable, ResourceManager)} の処理を、呼出し側が段階ごとに進められるよう分割したもの。
 *
 * <p>各段階は従来の一括処理と同じ順序・同じ内容で実行され、順序を飛ばす呼出しは {@link IllegalStateException}
 * で拒否する。起動ロード画面はフレームごとに一段階ずつ（MOD単位の段階は1MODずつ）進め、
 * 段階の合間に描画を継続する。MODのコードを呼ぶ段階はいずれも呼出しスレッド（GLスレッド）で実行される。</p>
 *
 * <p>途中で失敗した場合や中断した場合は {@link #abort()} を呼び、イベントチャネルを閉じること。</p>
 */
public final class BootstrapProcess {
    /**
     * 処理段階。宣言順に実行する。
     */
    public enum Stage {
        /**
         * 各MODの {@link GameMod#register(ModContext)}。
         */
        REGISTER_MODS,
        /**
         * 登録イベントの発行。
         */
        PUBLISH_REGISTRATIONS,
        /**
         * 設定JSONの読込みと解決。
         */
        LOAD_DATA,
        /**
         * 翻訳辞書の読込みと検証。
         */
        LOAD_LOCALIZATION,
        /**
         * キャラクターと保存済みカスタムキャラクターの登録。
         */
        REGISTER_CHARACTERS,
        /**
         * 能力ランタイムの生成とRegistryの凍結。
         */
        CREATE_ABILITIES,
        /**
         * 各MODの {@link GameMod#initialize(ModContext)}。
         */
        INITIALIZE_MODS,
        /**
         * 全段階が完了した。
         */
        COMPLETE
    }

    private final List<GameMod> mods;
    private final ResourceManager resources;
    private final EventChannels events = new EventChannels();
    private final RegistryManager manager = new RegistryManager();
    private final MojiBattleRegistries registries = new MojiBattleRegistries(manager);

    private Stage stage = Stage.REGISTER_MODS;
    private int modIndex;
    private boolean aborted;

    private List<CharacterDataRegistration> additionalCharacters;
    private List<SupportCharacterDataRegistration> additionalSupports;
    private List<FieldEffectDataRegistration> additionalFieldEffects;
    private ConfigManager config;
    private LocalizationManager i18n;
    private CustomCharacterStore customCharacters;
    private CharacterRegistry characters;
    private SupportRegistry supports;
    private AbilityRegistry abilities;

    BootstrapProcess(List<GameMod> mods, ResourceManager resources) {
        this.mods = mods;
        this.resources = resources;
    }

    /**
     * 次に実行する段階。
     */
    public Stage stage() {
        return stage;
    }

    /**
     * 読込み順に並べたMOD一覧。
     */
    public List<GameMod> mods() {
        return mods;
    }

    /**
     * MOD単位の段階で、次に処理するMODの位置（0始まり）。
     */
    public int modIndex() {
        return modIndex;
    }

    /**
     * MODを1件だけ登録する。全MODの登録が終わると {@link Stage#PUBLISH_REGISTRATIONS} へ進む。
     *
     * @return 登録したMODのID。MODが0件の場合はnull
     */
    public String registerNextMod() {
        require(Stage.REGISTER_MODS);
        String id = null;
        if (modIndex < mods.size()) {
            GameMod mod = mods.get(modIndex);
            id = mod.id();
            mod.register(context(mod));
            modIndex++;
        }
        if (modIndex >= mods.size()) {
            advance(Stage.PUBLISH_REGISTRATIONS);
        }
        return id;
    }

    public void publishRegistrations() {
        require(Stage.PUBLISH_REGISTRATIONS);
        manager.publishRegistrationEvents(events.lifecycle());
        events.lifecycle().post(new RegistriesRegisteredEvent(manager));

        additionalCharacters = List.copyOf(registries.characters.values());
        additionalSupports = List.copyOf(registries.supportCharacters.values());
        additionalFieldEffects = List.copyOf(registries.fieldEffects.values());
        advance(Stage.LOAD_DATA);
    }

    public void loadData() {
        require(Stage.LOAD_DATA);
        config = new ConfigManager(registries, resources, additionalCharacters, additionalSupports, additionalFieldEffects);
        events.lifecycle().post(new DataLoadedEvent(config));
        advance(Stage.LOAD_LOCALIZATION);
    }

    public void loadLocalization() {
        require(Stage.LOAD_LOCALIZATION);
        String language = config.game.language;
        i18n = new LocalizationManager(language, resources, Stream.concat(additionalCharacters.stream().map(registration -> registration.translation(language)), additionalSupports.stream().map(registration -> registration.translation(language))).distinct().toList());
        new TranslationDataValidator().validate(config, i18n);
        advance(Stage.REGISTER_CHARACTERS);
    }

    public void registerCharacters() {
        require(Stage.REGISTER_CHARACTERS);
        customCharacters = new CustomCharacterStore(config);
        characters = new CharacterRegistry();
        supports = new SupportRegistry();
        config.characters().forEach(characters::register);
        config.supports().forEach(supports::register);

        for (var custom : customCharacters.all()) {
            if (custom.type == CharacterType.MAIN) {
                characters.register(customCharacters.createMain(custom));
            } else {
                supports.register(customCharacters.createSupport(custom));
            }
        }
        advance(Stage.CREATE_ABILITIES);
    }

    public void createAbilities() {
        require(Stage.CREATE_ABILITIES);
        abilities = new AbilityRuntimeFactory(new RegistryResolver(registries)).create(config);
        manager.freezeAll();
        events.lifecycle().post(new RegistriesFrozenEvent(manager));
        modIndex = 0;
        advance(mods.isEmpty() ? Stage.COMPLETE : Stage.INITIALIZE_MODS);
    }

    /**
     * MODを1件だけ初期化する。全MODの初期化が終わると {@link Stage#COMPLETE} へ進む。
     *
     * @return 初期化したMODのID
     */
    public String initializeNextMod() {
        require(Stage.INITIALIZE_MODS);
        GameMod mod = mods.get(modIndex);
        mod.initialize(context(mod));
        modIndex++;
        if (modIndex >= mods.size()) {
            advance(Stage.COMPLETE);
        }
        return mod.id();
    }

    /**
     * 残りの段階を順に全て実行する。従来の一括起動と同じ結果になる。
     */
    public void runRemaining() {
        while (stage != Stage.COMPLETE) {
            switch (stage) {
                case REGISTER_MODS -> registerNextMod();
                case PUBLISH_REGISTRATIONS -> publishRegistrations();
                case LOAD_DATA -> loadData();
                case LOAD_LOCALIZATION -> loadLocalization();
                case REGISTER_CHARACTERS -> registerCharacters();
                case CREATE_ABILITIES -> createAbilities();
                case INITIALIZE_MODS -> initializeNextMod();
                case COMPLETE -> {
                }
            }
        }
    }

    /**
     * 全段階の完了後に、画面構築と戦闘開始に必要な共有サービスを返す。
     */
    public RegistrySnapshot snapshot() {
        require(Stage.COMPLETE);
        return new RegistrySnapshot(registries, config, i18n, characters, supports, abilities, customCharacters, events, resources);
    }

    /**
     * 読込み済みの設定。{@link Stage#LOAD_DATA} の完了前はnull。
     */
    public ConfigManager config() {
        return config;
    }

    /**
     * 失敗または中断時に、このプロセスが所有するイベントチャネルを閉じる。複数回呼び出してもよい。
     * {@link #snapshot()} を受け取った後はスナップショット側が所有するため呼ばないこと。
     */
    public void abort() {
        if (aborted) {
            return;
        }
        aborted = true;
        events.close();
    }

    private ModContext context(GameMod mod) {
        return new ModContext(mod.id(), scoped(events.lifecycle(), mod.id()), resources);
    }

    private void require(Stage expected) {
        if (aborted) {
            throw new IllegalStateException("Bootstrap process was aborted");
        }
        if (stage != expected) {
            throw new IllegalStateException("Bootstrap stage " + expected + " requested while at " + stage);
        }
    }

    private void advance(Stage next) {
        stage = next;
    }

    private static RegistrationBus scoped(RegistrationBus delegate, String namespace) {
        return new RegistrationBus() {
            @Override
            public String ownerNamespace() {
                return namespace;
            }

            @Override
            public <E> jp.takoa.mojibattle.event.EventSubscription subscribe(Class<E> eventType, java.util.function.Consumer<? super E> listener) {
                return delegate.subscribe(eventType, listener);
            }
        };
    }
}
