package jp.takoa.mojibattle.ability;

/** 外部MODがcore型を参照せずにAbility実装Factoryを登録する境界。 */
public interface AbilityImplementationRegistrar {
    /** MOD所有IDへ、装備者ごとに生成するスキルFactoryを登録する。 */
    void registerSkillImplementation(String implementationId, AbilityFactory<? extends SkillRuntime> factory);

    /** MOD所有IDへ、装備者ごとに生成する必殺Factoryを登録する。 */
    void registerUltimateImplementation(String implementationId, AbilityFactory<? extends UltimateRuntime> factory);

    /** MOD所有IDへ、装備者ごとに生成するパッシブFactoryを登録する。 */
    void registerPassiveImplementation(String implementationId, AbilityFactory<? extends PassiveRuntime> factory);

    /** MOD所有IDへ、補助個体ごとに生成するスキルFactoryを登録する。 */
    default void registerSupportSkillImplementation(String implementationId,
            AbilityFactory<? extends SupportSkillRuntime> factory) {
        throw new UnsupportedOperationException("Support skill registration is not supported by this host");
    }

    /** MOD所有IDへ、補助個体ごとに生成する必殺Factoryを登録する。 */
    default void registerSupportUltimateImplementation(String implementationId,
            AbilityFactory<? extends SupportUltimateRuntime> factory) {
        throw new UnsupportedOperationException("Support ultimate registration is not supported by this host");
    }
}
