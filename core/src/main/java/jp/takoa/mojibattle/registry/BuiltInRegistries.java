package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;

/**
 * 起動時に構築される定義・Factory・エフェクト各レジストリの集合。
 */
public final class BuiltInRegistries {
    public static final RegistryKey<SkillImplementation> SKILL_IMPLEMENTATIONS = RegistryKey.create("mojibattle:skill_implementations", SkillImplementation.class);
    public static final RegistryKey<UltimateImplementation> ULTIMATE_IMPLEMENTATIONS = RegistryKey.create("mojibattle:ultimate_implementations", UltimateImplementation.class);
    public static final RegistryKey<PassiveImplementation> PASSIVE_IMPLEMENTATIONS = RegistryKey.create("mojibattle:passive_implementations", PassiveImplementation.class);
    public static final RegistryKey<FieldEffectImplementation> FIELD_EFFECT_IMPLEMENTATIONS = RegistryKey.create("mojibattle:field_effect_implementations", FieldEffectImplementation.class);
    public static final RegistryKey<SupportAbilityImplementation> SUPPORT_ABILITY_IMPLEMENTATIONS = RegistryKey.create("mojibattle:support_ability_implementations", SupportAbilityImplementation.class);
    public static final RegistryKey<CombatEffectDefinition> EFFECTS = RegistryKey.create("mojibattle:effects", CombatEffectDefinition.class);
    public static final RegistryKey<AbilityConfigs.ProjectileSkill> SKILL_DEFINITIONS = RegistryKey.create("mojibattle:skill_definitions", AbilityConfigs.ProjectileSkill.class);
    public static final RegistryKey<AbilityConfigs.AreaUltimate> ULTIMATE_DEFINITIONS = RegistryKey.create("mojibattle:ultimate_definitions", AbilityConfigs.AreaUltimate.class);
    public static final RegistryKey<AbilityConfigs.TimedPassive> PASSIVE_DEFINITIONS = RegistryKey.create("mojibattle:passive_definitions", AbilityConfigs.TimedPassive.class);

    private final RegistryManager manager;

    public final Registry<SkillImplementation> skillImplementations;
    public final Registry<UltimateImplementation> ultimateImplementations;
    public final Registry<PassiveImplementation> passiveImplementations;
    public final Registry<FieldEffectImplementation> fieldEffectImplementations;
    public final Registry<SupportAbilityImplementation> supportAbilityImplementations;
    public final Registry<CombatEffectDefinition> effects;
    public final Registry<AbilityConfigs.ProjectileSkill> skillDefinitions;
    public final Registry<AbilityConfigs.AreaUltimate> ultimateDefinitions;
    public final Registry<AbilityConfigs.TimedPassive> passiveDefinitions;

    /**
     * 旧呼出し元との互換コンストラクタ。新規BootstrapはManagerを明示的に共有すること。
     */
    @Deprecated
    public BuiltInRegistries() {
        this(new RegistryManager());
    }

    public BuiltInRegistries(RegistryManager manager) {
        this.manager = manager;
        skillImplementations = manager.create(SKILL_IMPLEMENTATIONS, "skill implementation", SkillImplementation::id);
        ultimateImplementations = manager.create(ULTIMATE_IMPLEMENTATIONS, "ultimate implementation", UltimateImplementation::id);
        passiveImplementations = manager.create(PASSIVE_IMPLEMENTATIONS, "passive implementation", PassiveImplementation::id);
        fieldEffectImplementations = manager.create(FIELD_EFFECT_IMPLEMENTATIONS, "field-effect implementation", FieldEffectImplementation::id);
        supportAbilityImplementations = manager.create(SUPPORT_ABILITY_IMPLEMENTATIONS, "support ability implementation", SupportAbilityImplementation::id);
        effects = manager.create(EFFECTS, "combat effect", CombatEffectDefinition::id);
        skillDefinitions = manager.create(SKILL_DEFINITIONS, "skill definition", value -> value.id);
        ultimateDefinitions = manager.create(ULTIMATE_DEFINITIONS, "ultimate definition", value -> value.id);
        passiveDefinitions = manager.create(PASSIVE_DEFINITIONS, "passive definition", value -> value.id);
    }

    /**
     * 初期化完了後に変更してはならないレジストリを凍結する。
     */
    public void freeze() {
        manager.freezeAll();
    }

    public RegistryManager manager() {
        return manager;
    }

    public CombatEffectDefinition effectFromLegacyIndex(int index) {
        return effects.values().stream().filter(effect -> effect.legacyIndex() == index).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown legacy combat effect index: " + index));
    }
}
