package jp.takoa.mojibattle.item.ability.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigBoolean;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigList;
import jp.takoa.mojibattle.ability.config.annotations.ConfigResource;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.action.spec.FallingStrikeSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectilePatternSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileShotSpec;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.ArrayList;
import java.util.List;

/** 通常の複数弾・放射・ブーメラン・落下追撃を扱うスキル。 */
public final class ProjectileSkill extends AbstractRuntime<ProjectileSkill.Config> implements SkillRuntime {
    public ProjectileSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        Config config = getConfig();
        ArrayList<ProjectileShotSpec> shots = StandardProjectileSupport.shots(
                config.glyphs(), config.textures(), config.offsets(), List.of(), List.of(),
                config.baseDamage(), config.stunSeconds(), 0f);
        FallingStrikeSpec falling = null;
        if (context.target() != null && config.fallingObjectTexture() != null) {
            falling = new FallingStrikeSpec(context.target().position(), config.fallingRadius(),
                    DamageRequest.of(config.fallingDamage(), DamageType.SKILL), config.fallingWarningSeconds(),
                    config.fallingEffectSeconds(), config.fallingSpawnHeight(), config.activationEffect(),
                    config.hitEffect(), "building", config.fallingObjectTexture(), config.fallingHorizontalOffset(),
                    0f, 0f, 1f, 0f, 0f);
        }
        context.actions().spawnProjectilePattern(context.user(), new ProjectilePatternSpec(
                shots, config.projectileSpeed(), config.projectileRadius(), config.projectileLifetimeSeconds(),
                config.radial(), config.boomerang(), config.boomerangDistance(), config.ultimatePointsPerHit(),
                config.pointsOnHit(), config.glyphs().size() > 1 ? config.glyphs().get(1) : "",
                null, 0f, 0f, 0f, config.activationEffect(), config.projectileEffect(),
                config.activationTexture(), config.activationEffectScale(), falling, false));
        return true;
    }

    /** JSONから読み取る投射スキル設定。 */
    public record Config(
            @RequiredConfig @ConfigList(ConfigList.ValueType.STRING) List<String> glyphs,
            @RequiredConfig @ConfigList(ConfigList.ValueType.STRING) List<String> textures,
            @RequiredConfig @ConfigList(value = ConfigList.ValueType.FLOAT, dimensions = 2) List<List<Float>> offsets,
            @ConfigFloat(defaultValue = 0f) float baseDamage,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileSpeed,
            @ConfigFloat(defaultValue = .5f, exclusiveMin = true) float projectileRadius,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileLifetimeSeconds,
            @ConfigFloat(defaultValue = 0f) float stunSeconds,
            @ConfigBoolean(defaultValue = false) boolean radial,
            @ConfigBoolean(defaultValue = false) boolean boomerang,
            @ConfigBoolean(defaultValue = false) boolean pointsOnHit,
            @ConfigFloat(defaultValue = 0f) float boomerangDistance,
            @ConfigFloat(defaultValue = 0f) float ultimatePointsPerHit,
            @ConfigResource(name = "activationEffectId", defaultValue = "", nullable = true)
            ResourceId activationEffect,
            @ConfigResource(name = "projectileEffectId", defaultValue = "", nullable = true)
            ResourceId projectileEffect,
            @ConfigResource(name = "hitEffectId", defaultValue = "", nullable = true) ResourceId hitEffect,
            @ConfigResource(defaultValue = "", nullable = true) ResourceId activationTexture,
            @ConfigFloat(defaultValue = 0f) float activationEffectScale,
            @ConfigResource(defaultValue = "", nullable = true) ResourceId fallingObjectTexture,
            @ConfigFloat(defaultValue = 0f) float fallingRadius,
            @ConfigFloat(defaultValue = 0f) float fallingDamage,
            @ConfigFloat(defaultValue = 0f) float fallingWarningSeconds,
            @ConfigFloat(defaultValue = 0f) float fallingEffectSeconds,
            @ConfigFloat(defaultValue = 0f) float fallingSpawnHeight,
            @ConfigFloat(defaultValue = 0f) float fallingHorizontalOffset
    ) {
        public Config {
            StandardProjectileSupport.requireProjectileShape(glyphs, textures, offsets);
            if (fallingObjectTexture != null && fallingRadius <= 0f) {
                throw new IllegalArgumentException(
                        "fallingRadius must be positive when fallingObjectTexture is set");
            }
        }
    }
}
