package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.content.FieldEffectDataRegistration;
import jp.takoa.mojibattle.content.SupportCharacterDataRegistration;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.registry.impl.*;
import jp.takoa.mojibattle.resource.ResourceId;

/**
 * ホストとMODが共有する公開レジストリのキーと実体。
 */
public final class MojiBattleRegistries {
    public static final RegistryKey<SkillImpl> SKILL_IMPLEMENTATIONS = key("skill_implementations", SkillImpl.class);
    public static final RegistryKey<UltimateImpl> ULTIMATE_IMPLEMENTATIONS = key("ultimate_implementations", UltimateImpl.class);
    public static final RegistryKey<PassiveImpl> PASSIVE_IMPLEMENTATIONS = key("passive_implementations", PassiveImpl.class);
    public static final RegistryKey<FieldEffectImpl> FIELD_EFFECT_IMPLEMENTATIONS = key("field_effect_implementations", FieldEffectImpl.class);
    public static final RegistryKey<SupportAbilityImpl> SUPPORT_ABILITY_IMPLEMENTATIONS = key("support_ability_implementations", SupportAbilityImpl.class);
    public static final RegistryKey<SupportPassiveImpl> SUPPORT_PASSIVE_IMPLEMENTATIONS = key("support_passive_implementations", SupportPassiveImpl.class);
    public static final RegistryKey<CombatEffectDefinition> EFFECTS = key("effects", CombatEffectDefinition.class);
    public static final RegistryKey<CharacterDataRegistration> CHARACTERS = key("characters", CharacterDataRegistration.class);
    public static final RegistryKey<SupportCharacterDataRegistration> SUPPORT_CHARACTERS = key("support_characters", SupportCharacterDataRegistration.class);
    public static final RegistryKey<FieldEffectDataRegistration> FIELD_EFFECTS = key("field_effects", FieldEffectDataRegistration.class);
    public static final RegistryKey<AbilityConfigs.ProjectileSkill> SKILL_DEFINITIONS = key("skill_definitions", AbilityConfigs.ProjectileSkill.class);
    public static final RegistryKey<AbilityConfigs.AreaUltimate> ULTIMATE_DEFINITIONS = key("ultimate_definitions", AbilityConfigs.AreaUltimate.class);
    public static final RegistryKey<AbilityConfigs.TimedPassive> PASSIVE_DEFINITIONS = key("passive_definitions", AbilityConfigs.TimedPassive.class);

    private final RegistryManager manager;
    public final Registry<SkillImpl> skillImplementations;
    public final Registry<UltimateImpl> ultimateImplementations;
    public final Registry<PassiveImpl> passiveImplementations;
    public final Registry<FieldEffectImpl> fieldEffectImplementations;
    public final Registry<SupportAbilityImpl> supportAbilityImplementations;
    public final Registry<SupportPassiveImpl> supportPassiveImplementations;
    public final Registry<CombatEffectDefinition> effects;
    public final Registry<CharacterDataRegistration> characters;
    public final Registry<SupportCharacterDataRegistration> supportCharacters;
    public final Registry<FieldEffectDataRegistration> fieldEffects;
    public final Registry<AbilityConfigs.ProjectileSkill> skillDefinitions;
    public final Registry<AbilityConfigs.AreaUltimate> ultimateDefinitions;
    public final Registry<AbilityConfigs.TimedPassive> passiveDefinitions;

    public MojiBattleRegistries(RegistryManager manager) {
        this.manager = manager;
        skillImplementations = manager.create(SKILL_IMPLEMENTATIONS, "skill implementation", SkillImpl::id);
        ultimateImplementations = manager.create(ULTIMATE_IMPLEMENTATIONS, "ultimate implementation", UltimateImpl::id);
        passiveImplementations = manager.create(PASSIVE_IMPLEMENTATIONS, "passive implementation", PassiveImpl::id);
        fieldEffectImplementations = manager.create(FIELD_EFFECT_IMPLEMENTATIONS, "field-effect implementation", FieldEffectImpl::id);
        supportAbilityImplementations = manager.create(SUPPORT_ABILITY_IMPLEMENTATIONS, "support ability implementation", SupportAbilityImpl::id);
        supportPassiveImplementations = manager.create(SUPPORT_PASSIVE_IMPLEMENTATIONS, "support passive implementation", SupportPassiveImpl::id);
        effects = manager.create(EFFECTS, "combat effect", CombatEffectDefinition::id);
        characters = manager.create(CHARACTERS, "character data",
                value -> value.character().toString(),
                (id, value) -> validateResourceOwner("character", value.getClass(), id, value.character()));
        supportCharacters = manager.create(SUPPORT_CHARACTERS, "support character data",
                value -> value.support().toString(),
                (id, value) -> validateResourceOwner("support character", value.getClass(), id, value.support()));
        fieldEffects = manager.create(FIELD_EFFECTS, "field effect data",
                value -> value.fieldEffect().toString(),
                (id, value) -> validateResourceOwner("field effect", value.getClass(), id, value.fieldEffect()));
        skillDefinitions = manager.create(SKILL_DEFINITIONS, "skill definition", value -> value.id);
        ultimateDefinitions = manager.create(ULTIMATE_DEFINITIONS, "ultimate definition", value -> value.id);
        passiveDefinitions = manager.create(PASSIVE_DEFINITIONS, "passive definition", value -> value.id);
    }

    public RegistryManager manager() {
        return manager;
    }

    public CombatEffectDefinition effectFromLegacyIndex(int index) {
        return effects.values().stream().filter(effect -> effect.legacyIndex() == index).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown legacy combat effect index: " + index));
    }

    private static <T> RegistryKey<T> key(String path, Class<T> type) {
        return RegistryKey.create("mojibattle:" + path, type);
    }

    /** 登録名とリソースパスは独立させつつ、MOD所有境界だけを検証する。 */
    private static void validateResourceOwner(String type, Class<?> valueType,
                                              RegistryId registrationId, ResourceId resourceId) {
        if (!registrationId.namespace().equals(resourceId.namespace())) {
            throw new IllegalArgumentException("Invalid registration: registry=" + type + " data"
                    + ", valueType=" + valueType.getName()
                    + ", registrationId=" + registrationId
                    + ", resourceId=" + resourceId
                    + ", rule=Resource namespace must match registration namespace");
        }
    }
}
