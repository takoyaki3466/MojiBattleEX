package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime;
import jp.takoa.mojibattle.ability.runtime.UltimateRuntime;
import jp.takoa.mojibattle.event.EventSubscription;
import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.event.lifecycle.RegisterEvent;
import jp.takoa.mojibattle.registry.impl.PassiveImpl;
import jp.takoa.mojibattle.registry.impl.SkillImpl;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.registry.impl.SupportPassiveImpl;
import jp.takoa.mojibattle.registry.impl.UltimateImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 登録宣言と実体生成を分離し、登録イベントで一括確定する。
 *
 * @param <T> 登録要素型
 */
public class MojiBattleRegister<T> {
    private static final Consumer<ConfigView> NO_VALIDATION = ignored -> {
    };

    private final RegistryKey<T> registryKey;
    private final String namespace;
    private final Map<RegistryHandle<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();

    private EventSubscription subscription;
    private boolean resolved;

    protected MojiBattleRegister(RegistryKey<T> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = RegistryId.of(namespace, "validation").namespace();
    }

    /**
     * 任意の公開レジストリへ登録する汎用レジスターを作成する。
     */
    public static <T> MojiBattleRegister<T> create(RegistryKey<T> registryKey, String namespace) {
        return new MojiBattleRegister<>(registryKey, namespace);
    }

    /**
     * スキル実装用の型付きレジスターを作成する。
     */
    public static Skill createSkill(String namespace) {
        return new Skill(namespace);
    }

    /**
     * 必殺技実装用の型付きレジスターを作成する。
     */
    public static Ultimate createUltimate(String namespace) {
        return new Ultimate(namespace);
    }

    /**
     * パッシブ実装用の型付きレジスターを作成する。
     */
    public static Passive createPassive(String namespace) {
        return new Passive(namespace);
    }

    /**
     * 補助スキル・補助必殺技共用の型付きレジスターを作成する。
     */
    public static SupportAbility createSupportAbility(String namespace) {
        return new SupportAbility(namespace);
    }

    /**
     * 補助パッシブ実装用の型付きレジスターを作成する。
     */
    public static SupportPassive createSupportPassive(String namespace) {
        return new SupportPassive(namespace);
    }

    /**
     * 従来どおり、登録要素を生成する任意のFactoryで宣言する。
     */
    public RegistryHandle<T> register(String path, Supplier<? extends T> factory) {
        if (subscription != null || resolved) {
            throw new IllegalStateException("Cannot declare entries after DeferredRegister attachment");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Registry factory is required");
        }
        RegistryHandle<T> handle = new RegistryHandle<>(RegistryId.of(namespace, path));
        if (entries.keySet().stream().anyMatch(existing -> existing.id().equals(handle.id()))) {
            throw new IllegalArgumentException("Duplicate deferred registration: " + handle.id());
        }
        entries.put(handle, factory);
        return handle;
    }

    /**
     * 登録イベントを受け取るため、購読専用バスへ一度だけ接続する。
     */
    public void attach(RegistrationBus registrationBus) {
        if (subscription != null) {
            throw new IllegalStateException("DeferredRegister is already attached: " + registryKey.id());
        }

        if (registrationBus == null) {
            throw new IllegalArgumentException("Registration bus is required");
        }

        String owner = registrationBus.ownerNamespace();

        if (owner != null && !owner.equals(namespace)) {
            throw new IllegalArgumentException("MOD " + owner + " cannot attach registrations for namespace " + namespace);
        }

        subscription = registrationBus.subscribe(RegisterEvent.class, this::onRegister);
    }

    public RegistryKey<T> registryKey() {
        return registryKey;
    }

    public Collection<RegistryHandle<T>> entries() {
        return List.copyOf(entries.keySet());
    }

    protected final String registrationId(String path) {
        return RegistryId.of(namespace, path).toString();
    }

    /**
     * 全Factoryを先に評価・検証し、成功した場合だけ登録とHandle束縛を行う。
     */
    @SuppressWarnings("unchecked")
    private void onRegister(RegisterEvent<?> rawEvent) {
        if (!registryKey.equals(rawEvent.key())) {
            return;
        }

        if (resolved) {
            throw new IllegalStateException("DeferredRegister was resolved twice: " + registryKey.id());
        }

        Registry<T> registry = (Registry<T>) rawEvent.registry();
        List<Map.Entry<RegistryHandle<T>, T>> staged = new ArrayList<>();

        entries.forEach((key, value1) -> {
            T value = value1.get();
            registry.validate(key.id(), value);
            if (staged.stream().anyMatch(valueEntry -> valueEntry.getKey().id().equals(key.id()))) {
                throw new IllegalArgumentException("Duplicate staged registration: " + key.id());
            }
            staged.add(Map.entry(key, value));
        });

        staged.forEach(entry -> registry.register(entry.getKey().id(), entry.getValue()));
        staged.forEach(entry -> entry.getKey().bind(entry.getValue()));

        resolved = true;
        subscription.close();
    }

    /**
     * スキルの簡易登録と従来の詳細登録を提供する。
     */
    public static class Skill extends MojiBattleRegister<SkillImpl> {
        public Skill(String namespace) {
            super(MojiBattleRegistries.SKILL_IMPLEMENTATIONS, namespace);
        }

        /**
         * publicな{@code (ConfigRecord)}コンストラクタからスキルを登録する。
         */
        public <R extends SkillRuntime> RegistryHandle<SkillImpl> register(String path, Class<R> runtimeType) {
            return register(path, runtimeType, NO_VALIDATION);
        }

        /**
         * Config自動検証に、複数項目間などの追加検証を合成して登録する。
         */
        public <R extends SkillRuntime> RegistryHandle<SkillImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator) {
            RuntimeClassRegistration<SkillRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), SkillRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), (source, multiplier) -> AbilityPresentation.fromDescription(source, registration.displayValues(source, multiplier)));
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを、追加検証なしで組み合わせて登録する。
         */
        public <R extends SkillRuntime> RegistryHandle<SkillImpl> registerWithPresentation(String path, Class<R> runtimeType, BiFunction<ConfigView, Float, AbilityPresentation> presentation) {
            return register(path, runtimeType, NO_VALIDATION, presentation);
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを組み合わせて登録する。
         */
        public <R extends SkillRuntime> RegistryHandle<SkillImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator, BiFunction<ConfigView, Float, AbilityPresentation> presentation) {
            RuntimeClassRegistration<SkillRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), SkillRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), presentation);
        }

        public RegistryHandle<SkillImpl> register(String path, AbilityFactory<? extends SkillRuntime> factory, Consumer<ConfigView> validator, BiFunction<ConfigView, Float, AbilityPresentation> presentation) {
            String id = registrationId(path);
            return register(path, () -> new SkillImpl(id, factory, validator, presentation));
        }

        public <C> RegistryHandle<SkillImpl> register(String path, Function<ConfigView, C> configFactory, Function<C, ? extends SkillRuntime> runtimeFactory) {
            return register(path, source -> runtimeFactory.apply(configFactory.apply(source)), configFactory::apply, (source, multiplier) -> AbilityPresentation.fromDescription(source, 0f));
        }
    }

    /**
     * 必殺技の簡易登録と詳細登録を提供する。
     */
    public static final class Ultimate extends MojiBattleRegister<UltimateImpl> {
        public Ultimate(String namespace) {
            super(MojiBattleRegistries.ULTIMATE_IMPLEMENTATIONS, namespace);
        }

        /**
         * publicな{@code (ConfigRecord)}コンストラクタから必殺技を登録する。
         */
        public <R extends UltimateRuntime> RegistryHandle<UltimateImpl> register(String path, Class<R> runtimeType) {
            return register(path, runtimeType, NO_VALIDATION);
        }

        public <R extends UltimateRuntime> RegistryHandle<UltimateImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator) {
            RuntimeClassRegistration<UltimateRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), UltimateRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), source -> AbilityPresentation.fromDescription(source, registration.displayValues(source, 1f)));
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを、追加検証なしで組み合わせて登録する。
         */
        public <R extends UltimateRuntime> RegistryHandle<UltimateImpl> registerWithPresentation(String path, Class<R> runtimeType, Function<ConfigView, AbilityPresentation> presentation) {
            return register(path, runtimeType, NO_VALIDATION, presentation);
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを組み合わせて登録する。
         */
        public <R extends UltimateRuntime> RegistryHandle<UltimateImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator, Function<ConfigView, AbilityPresentation> presentation) {
            RuntimeClassRegistration<UltimateRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), UltimateRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), presentation);
        }

        public RegistryHandle<UltimateImpl> register(String path, AbilityFactory<? extends UltimateRuntime> factory, Consumer<ConfigView> validator, Function<ConfigView, AbilityPresentation> presentation) {
            String id = registrationId(path);
            return register(path, () -> new UltimateImpl(id, factory, validator, presentation));
        }

        public <C> RegistryHandle<UltimateImpl> register(String path, Function<ConfigView, C> configFactory, Function<C, ? extends UltimateRuntime> runtimeFactory) {
            return register(path, source -> runtimeFactory.apply(configFactory.apply(source)), configFactory::apply, source -> AbilityPresentation.fromDescription(source));
        }
    }

    /**
     * メインパッシブの簡易登録と詳細登録を提供する。
     */
    public static final class Passive extends MojiBattleRegister<PassiveImpl> {
        public Passive(String namespace) {
            super(MojiBattleRegistries.PASSIVE_IMPLEMENTATIONS, namespace);
        }

        /**
         * publicな{@code (ConfigRecord)}コンストラクタからメインパッシブを登録する。
         */
        public <R extends PassiveRuntime> RegistryHandle<PassiveImpl> register(String path, Class<R> runtimeType) {
            return register(path, runtimeType, NO_VALIDATION);
        }

        public <R extends PassiveRuntime> RegistryHandle<PassiveImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator) {
            RuntimeClassRegistration<PassiveRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), PassiveRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), source -> AbilityPresentation.fromDescription(source, registration.displayValues(source, 1f)));
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを、追加検証なしで組み合わせて登録する。
         */
        public <R extends PassiveRuntime> RegistryHandle<PassiveImpl> registerWithPresentation(String path, Class<R> runtimeType, Function<ConfigView, AbilityPresentation> presentation) {
            return register(path, runtimeType, NO_VALIDATION, presentation);
        }

        /**
         * Runtimeクラスの簡易生成と独自Presentationを組み合わせて登録する。
         */
        public <R extends PassiveRuntime> RegistryHandle<PassiveImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator, Function<ConfigView, AbilityPresentation> presentation) {
            RuntimeClassRegistration<PassiveRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), PassiveRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator(), presentation);
        }

        public RegistryHandle<PassiveImpl> register(String path, AbilityFactory<? extends PassiveRuntime> factory, Consumer<ConfigView> validator, Function<ConfigView, AbilityPresentation> presentation) {
            String id = registrationId(path);
            return register(path, () -> new PassiveImpl(id, factory, validator, presentation));
        }

        public <C> RegistryHandle<PassiveImpl> register(String path, Function<ConfigView, C> configFactory, Function<C, ? extends PassiveRuntime> runtimeFactory) {
            return register(path, source -> runtimeFactory.apply(configFactory.apply(source)), configFactory::apply, source -> AbilityPresentation.fromDescription(source));
        }
    }

    /**
     * 補助スキルと補助必殺技に共通する簡易登録と詳細登録を提供する。
     */
    public static final class SupportAbility extends MojiBattleRegister<SupportAbilityImpl> {
        public SupportAbility(String namespace) {
            super(MojiBattleRegistries.SUPPORT_ABILITY_IMPLEMENTATIONS, namespace);
        }

        /**
         * publicな{@code (ConfigRecord)}コンストラクタから補助発動能力を登録する。
         */
        public <R extends SupportAbilityRuntime> RegistryHandle<SupportAbilityImpl> register(String path, Class<R> runtimeType) {
            return register(path, runtimeType, NO_VALIDATION);
        }

        public <R extends SupportAbilityRuntime> RegistryHandle<SupportAbilityImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator) {
            RuntimeClassRegistration<SupportAbilityRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), SupportAbilityRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator());
        }

        public RegistryHandle<SupportAbilityImpl> register(String path, AbilityFactory<? extends SupportAbilityRuntime> factory, Consumer<ConfigView> validator) {
            String id = registrationId(path);
            return register(path, () -> new SupportAbilityImpl(id, factory, validator));
        }

        public <C> RegistryHandle<SupportAbilityImpl> register(String path, Function<ConfigView, C> configFactory, Function<C, ? extends SupportAbilityRuntime> runtimeFactory) {
            AbilityFactory<? extends SupportAbilityRuntime> factory = source -> runtimeFactory.apply(configFactory.apply(source));
            Consumer<ConfigView> validator = configFactory::apply;

            return register(path, factory, validator);
        }
    }

    /**
     * 補助パッシブの簡易登録と詳細登録を提供する。
     */
    public static final class SupportPassive extends MojiBattleRegister<SupportPassiveImpl> {
        public SupportPassive(String namespace) {
            super(MojiBattleRegistries.SUPPORT_PASSIVE_IMPLEMENTATIONS, namespace);
        }

        /**
         * publicな{@code (ConfigRecord)}コンストラクタから補助パッシブを登録する。
         */
        public <R extends SupportPassiveRuntime> RegistryHandle<SupportPassiveImpl> register(String path, Class<R> runtimeType) {
            return register(path, runtimeType, NO_VALIDATION);
        }

        public <R extends SupportPassiveRuntime> RegistryHandle<SupportPassiveImpl> register(String path, Class<R> runtimeType, Consumer<ConfigView> additionalValidator) {
            RuntimeClassRegistration<SupportPassiveRuntime> registration = new RuntimeClassRegistration<>(registrationId(path), SupportPassiveRuntime.class, runtimeType, additionalValidator);
            return register(path, registration.factory(), registration.validator());
        }

        public RegistryHandle<SupportPassiveImpl> register(String path, AbilityFactory<? extends SupportPassiveRuntime> factory, Consumer<ConfigView> validator) {
            String id = registrationId(path);
            return register(path, () -> new SupportPassiveImpl(id, factory, validator));
        }

        public <C> RegistryHandle<SupportPassiveImpl> register(String path, Function<ConfigView, C> configFactory, Function<C, ? extends SupportPassiveRuntime> runtimeFactory) {
            AbilityFactory<? extends SupportPassiveRuntime> factory = source -> runtimeFactory.apply(configFactory.apply(source));
            Consumer<ConfigView> validator = configFactory::apply;

            return register(path, factory, validator);
        }
    }
}
