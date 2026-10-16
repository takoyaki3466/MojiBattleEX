package jp.takoa.mojibattle.data.validator;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.config.ControlConfig;
import jp.takoa.mojibattle.config.FieldEffectDefinition;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.CombatEffectDefinition;
import jp.takoa.mojibattle.registry.RegistryId;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * デシリアライズ済みデータの数値範囲、参照ID、アセット、入力重複を検証する。
 * 読込や実行インスタンス生成を行わず、失敗時は対象IDを含む例外を返す。
 */
public final class GameDataValidator {
    private final BuiltInRegistries registries;
    private final ResourceManager resources;

    public GameDataValidator(BuiltInRegistries registries, ResourceManager resources) {
        this.registries = registries;
        this.resources = resources;
    }

    /**
     * 起動時に読み込んだ設定全体を横断し、数値制約、Registry参照、アセット参照を検証する。
     * 途中で補正は行わず、最初に見つかった不整合を例外として報告する。
     *
     * @param config デシリアライズと旧形式の正規化を終えた設定
     * @throws IllegalArgumentException 設定値または参照関係が不正な場合
     */
    public void validate(ConfigManager config) {
        validateGlobalSettings(config);
        for (CombatEffectDefinition effect : registries.effects.values()) {
            require(assetExists(effect.texturePath()), "Combat effect texture is missing: " + effect.id());
        }
        for (CharacterDefinition character : config.characters()) {
            validateCharacter(config, character);
        }
        for (SupportCharacterDefinition support : config.supports()) {
            validateSupport(config, support);
        }
        for (FieldEffectDefinition effect : config.fieldEffects()) {
            validateFieldEffect(effect);
        }
        validateBindings(config.controls);
    }

    private void validateGlobalSettings(ConfigManager config) {
        require(config.customEditor.maximumSlots > 0 && config.customEditor.maximumMainPassives > 0 && config.customEditor.maximumSupportFieldPassives > 0 && config.customEditor.saveVersion > 0, "custom editor config is invalid");
        require(config.battle.durationSeconds > 0 && config.battle.fixedStepSeconds > 0, "battle times must be positive");
        require(config.battle.skillCooldownMultiplier > 0
                        && config.battle.skillCooldownMultiplier <= 1
                        && config.battle.healthMultiplier >= 1
                        && config.battle.chaosHealthMultiplier >= 1,
                "battle pacing multipliers are invalid");
        require(config.battle.finalMinuteSeconds > 0 && config.battle.finalMinuteSeconds < config.battle.durationSeconds, "final minute threshold is invalid");
        require(config.critical.baseRate >= 0 && config.critical.baseRate <= 1 && config.critical.baseDamageMultiplier >= 1, "critical config is invalid");
        require(config.awakening.spawnAtRemainingSeconds > 0 && config.awakening.spawnAtRemainingSeconds < config.battle.durationSeconds && config.awakening.attackMultiplier >= 1 && config.awakening.defenseMultiplier >= 1 && config.awakening.moveSpeedMultiplier >= 1 && config.awakening.ultimateGainMultiplier >= 1 && config.awakening.pickupRadius > 0 && config.awakening.preferredDistance > config.awakening.pickupRadius && assetExists(config.awakening.texturePath), "awakening config is invalid");
    }

    /** キャラクターが参照するAbility実装と演出アセットを、用途を含めて検証する。 */
    private void validateCharacter(ConfigManager config, CharacterDefinition character) {
        AbilityConfigs.ProjectileSkill skill = registries.skillDefinitions.require(character.skillId);
        AbilityConfigs.AreaUltimate ultimate = registries.ultimateDefinitions.require(character.ultimateId);
        AbilityConfigs.TimedPassive passive = registries.passiveDefinitions.require(character.passiveId);
        boolean externalSkill = externalImplementation(skill.implementationId);
        boolean externalUltimate = externalImplementation(ultimate.implementationId);
        boolean externalPassive = externalImplementation(passive.implementationId);
        require(character.maxHp > 0 && character.moveSpeed > 0 && character.collisionRadius > 0, "Character stats must be positive: " + character.id);
        require(character.type == CharacterType.MAIN, "Playable character must be MAIN: " + character.id);
        require(assetExists(character.glyphTexture), "Character glyph texture is missing: " + character.id);
        registries.skillImplementations.require(skill.implementationId).validator().accept(skill);
        registries.ultimateImplementations.require(ultimate.implementationId).validator().accept(ultimate);
        registries.passiveImplementations.require(passive.implementationId).validator().accept(passive);
        require(skill.nameKey != null && ultimate.nameKey != null && passive.nameKey != null, "Ability translation key is missing: " + character.id);
        requireOptionalEffect(skill.activationEffectId, CombatEffectDefinition.Usage.SKILL, externalSkill);
        requireOptionalEffect(skill.projectileEffectId, CombatEffectDefinition.Usage.PROJECTILE, externalSkill);
        requireOptionalEffect(skill.hitEffectId, CombatEffectDefinition.Usage.HIT, externalSkill);
        requireOptionalEffect(ultimate.warningEffectId, CombatEffectDefinition.Usage.ULTIMATE_WARNING, externalUltimate);
        requireOptionalEffect(ultimate.impactEffectId, CombatEffectDefinition.Usage.ULTIMATE_IMPACT, externalUltimate);
        requireOptionalEffect(ultimate.renderEffectId, CombatEffectDefinition.Usage.GENERAL, externalUltimate);
        for (String texture : skill.textures == null ? new String[0] : skill.textures) {
            require(assetExists(texture), "Skill texture is missing: " + character.id);
        }
        require(skill.followUpTexture == null || assetExists(skill.followUpTexture), "Follow-up texture is missing: " + character.id);
        require(skill.activationTexture == null || assetExists(skill.activationTexture), "Skill activation texture is missing: " + character.id);
        require(skill.fallingObjectTexture == null || assetExists(skill.fallingObjectTexture), "Falling object texture is missing: " + character.id);
        require(ultimate.pointCost > 0 && ultimate.warningSeconds >= 0
                && (externalUltimate || assetExists(ultimate.objectTexture)), "Ultimate common config is invalid: " + character.id);
        if (ultimate.burnDuration > 0) {
            require(ultimate.burnDamage > 0 && ultimate.burnTickInterval > 0 && ultimate.burnUltimatePointsPerTick >= 0, "Burn config is invalid: " + character.id);
        }
        if (ultimate.barrageSkillId != null) {
            registries.skillDefinitions.require(ultimate.barrageSkillId);
        }
        require(externalPassive || (passive.intervalSeconds > 0 && passive.maximumMultiplier >= 1 && passive.moveSpeedMultiplier > 0 && passive.skillCooldownMultiplier > 0 && passive.ultimateGainMultiplier > 0 && passive.incomingDamageMultiplier > 0 && passive.hitboxMultiplier > 0 && passive.stunMultiplier > 0 && passive.ultimatePointsPerSecond >= 0), "Passive common config is invalid: " + character.id);
        require(passive.spawnObjectTexture == null || assetExists(passive.spawnObjectTexture), "Passive object texture is missing: " + character.id);
    }

    /** 補助キャラクター固有の能力値、実装ID、フィールド効果参照を検証する。 */
    private void validateSupport(ConfigManager config, SupportCharacterDefinition support) {
        require(support.type == CharacterType.SUPPORT && support.moveSpeed > 0 && support.followDistance > 0 && support.attackRange > 0 && support.attackInterval > 0 && support.projectileSpeed > 0 && support.ultimateChargePerSecond > 0, "Support stats are invalid: " + support.id);
        require(support.skill != null && support.ultimate != null && support.fieldPassiveIds != null && support.fieldPassiveIds.length > 0, "Support abilities are missing: " + support.id);
        require(assetExists(support.glyphTexture) && assetExists(support.projectileTexture), "Support texture is missing: " + support.id);
        registries.supportAbilityImplementations.require(support.skill.type).validator().accept(support.skill);
        registries.supportAbilityImplementations.require(support.ultimate.type).validator().accept(support.ultimate);
        if (support.supportPassiveImplementationId != null) {
            registries.supportPassiveImplementations.require(support.supportPassiveImplementationId);
        }
        require(support.skill.cooldownSeconds > 0 && support.ultimate.pointCost == 100, "Support ability values are invalid: " + support.id);
        for (String effectId : support.fieldPassiveIds) {
            config.fieldEffect(effectId);
        }
    }

    private void requireEffect(String id, CombatEffectDefinition.Usage usage) {
        CombatEffectDefinition effect = registries.effects.require(id);
        require(effect.usages().contains(usage) || effect.usages().contains(CombatEffectDefinition.Usage.GENERAL), "Combat effect " + id + " cannot be used as " + usage);
    }

    private void requireOptionalEffect(String id, CombatEffectDefinition.Usage usage, boolean optional) {
        if (id == null && optional) {
            return;
        }
        requireEffect(id, usage);
    }

    private void validateFieldEffect(FieldEffectDefinition effect) {
        require(effect.id != null && effect.type != null && effect.target != null, "Field effect fields are required");
        String target = effect.target.toUpperCase(java.util.Locale.ROOT);
        require(java.util.Set.of("ALL", "ALLIES", "ENEMIES", "MAIN", "SUPPORT").contains(target),
                "Unknown field effect target: " + effect.target);
        require(effect.includeCharacterIds != null && effect.excludeCharacterIds != null,
                "Field effect include/exclude filters are required");
        registries.fieldEffectImplementations.require(effect.type).validator().accept(effect);
    }

    /** 操作キーが4プレイヤーと一時停止を通して重複しないことを検証する。 */
    private static void validateBindings(ControlConfig controls) {
        Set<String> keys = new HashSet<>();
        addBindings(keys, controls.playerOne);
        addBindings(keys, controls.playerTwo);
        addBindings(keys, controls.playerThree);
        addBindings(keys, controls.playerFour);
        require(keys.add(controls.pause), "duplicate key binding: " + controls.pause);
    }

    private boolean assetExists(String path) {
        if (path == null) {
            return false;
        }
        try {
            return resources.exists(ResourceKind.ASSET, ResourceId.parse(path));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static void addBindings(Set<String> keys, ControlConfig.Binding binding) {
        require(binding != null, "control binding is missing");
        for (String key : new String[]{binding.forward, binding.backward, binding.left, binding.right, binding.skill, binding.ultimate, binding.supportSkill, binding.supportUltimate}) {
            require(key != null && keys.add(key), "missing or duplicate key binding: " + key);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean externalImplementation(String id) {
        return !RegistryId.DEFAULT_NAMESPACE.equals(RegistryId.parse(id).namespace());
    }
}
