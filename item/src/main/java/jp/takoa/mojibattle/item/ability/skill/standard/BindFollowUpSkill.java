package jp.takoa.mojibattle.item.ability.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigBoolean;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigList;
import jp.takoa.mojibattle.ability.config.annotations.ConfigResource;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.action.spec.ProjectilePatternSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileShotSpec;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * 単発弾へ個別ダメージ・拘束時間と追撃弾を付与するスキル。
 */
public final class BindFollowUpSkill extends AbstractRuntime<BindFollowUpSkill.Config> implements SkillRuntime {
    public BindFollowUpSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        Config config = getConfig();
        List<ProjectileShotSpec> shots = StandardProjectileSupport.shots(
                config.glyphs(), config.textures(), config.offsets(), config.damages(), config.bindDurations(),
                config.baseDamage(), config.stunSeconds(), config.bindSeconds());
        context.actions().spawnProjectilePattern(context.user(), new ProjectilePatternSpec(
                List.of(shots.getFirst()), config.projectileSpeed(), config.projectileRadius(),
                config.projectileLifetimeSeconds(), false, false, 0f, config.ultimatePointsPerHit(),
                config.pointsOnHit(), config.glyphs().get(1), config.followUpTexture(), config.followUpDamage(),
                config.followUpRadius(), config.followUpSpeed(), config.activationEffect(),
                config.projectileEffect(), null, 0f, null, false));
        return true;
    }

    /**
     * JSONから読み取る拘束追撃スキル設定。
     */
    public record Config(
            @RequiredConfig @ConfigList(ConfigList.ValueType.STRING) List<String> glyphs,
            @RequiredConfig @ConfigList(ConfigList.ValueType.STRING) List<String> textures,
            @RequiredConfig @ConfigList(value = ConfigList.ValueType.FLOAT, dimensions = 2) List<List<Float>> offsets,
            @ConfigList(ConfigList.ValueType.FLOAT) List<Float> damages,
            @ConfigList(ConfigList.ValueType.FLOAT) List<Float> bindDurations,
            @ConfigFloat(defaultValue = 0f) float baseDamage,
            @ConfigFloat(defaultValue = 0f) float stunSeconds,
            @ConfigFloat(defaultValue = 0f) float bindSeconds,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileSpeed,
            @ConfigFloat(defaultValue = .5f, exclusiveMin = true) float projectileRadius,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileLifetimeSeconds,
            @ConfigFloat(defaultValue = 0f) float ultimatePointsPerHit,
            @ConfigBoolean(defaultValue = false) boolean pointsOnHit,
            @ConfigResource(name = "activationEffectId", defaultValue = "", nullable = true)
            ResourceId activationEffect,
            @ConfigResource(name = "projectileEffectId", defaultValue = "", nullable = true)
            ResourceId projectileEffect,
            @ConfigResource(defaultValue = "", nullable = true) ResourceId followUpTexture,
            @ConfigFloat(defaultValue = 0f) float followUpDamage,
            @ConfigFloat(defaultValue = 0f) float followUpRadius,
            @ConfigFloat(defaultValue = 0f) float followUpSpeed
    ) {
        public Config {
            StandardProjectileSupport.requireProjectileShape(glyphs, textures, offsets);
            if (glyphs.size() < 2 || damages.size() != glyphs.size()
                    || bindDurations.size() != glyphs.size() || followUpTexture == null
                    || followUpDamage <= 0f || followUpRadius <= 0f || followUpSpeed <= 0f) {
                throw new IllegalArgumentException("Bind follow-up skill config is invalid");
            }
        }
    }
}
