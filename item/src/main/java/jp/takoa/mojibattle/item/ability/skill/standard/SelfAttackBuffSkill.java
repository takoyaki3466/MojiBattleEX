package jp.takoa.mojibattle.item.ability.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigList;
import jp.takoa.mojibattle.ability.config.annotations.ConfigResource;
import jp.takoa.mojibattle.ability.config.annotations.ConfigString;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.action.spec.ProjectileSpec;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 自身へ攻撃強化を付与し、対象がいれば無害な演出弾を放つスキル。 */
public final class SelfAttackBuffSkill extends AbstractRuntime<SelfAttackBuffSkill.Config>
        implements SkillRuntime {
    public SelfAttackBuffSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        Config config = getConfig();
        context.actions().applyStatus(context.user(), context.user(), StandardProjectileSupport.status(
                config.id() + "/attack", StatusEffectType.ATTACK_MULTIPLIER,
                config.attackMultiplier(), config.durationSeconds()));
        if (context.target() != null) {
            context.actions().spawnProjectile(context.user(), context.target(), new ProjectileSpec(
                    DamageRequest.of(0f, DamageType.SKILL), config.projectileSpeed(), config.projectileRadius(),
                    config.projectileLifetimeSeconds(), 5, 20f, 1,
                    config.textures().isEmpty()
                            ? null : StandardProjectileSupport.resource(config.textures().getFirst()),
                    config.projectileEffect()));
        }
        context.actions().addUltimatePoints(context.user(), config.ultimatePoints());
        return true;
    }

    /** JSONから読み取る自己強化スキル設定。 */
    public record Config(
            @RequiredConfig @ConfigString(defaultValue = "ability") String id,
            @ConfigFloat(name = "buffAttackMultiplier", defaultValue = 1f) float attackMultiplier,
            @ConfigFloat(name = "buffDurationSeconds", defaultValue = 1f, exclusiveMin = true)
            float durationSeconds,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileSpeed,
            @ConfigFloat(defaultValue = .5f, exclusiveMin = true) float projectileRadius,
            @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileLifetimeSeconds,
            @ConfigFloat(name = "ultimatePointsPerHit", defaultValue = 0f) float ultimatePoints,
            @ConfigList(ConfigList.ValueType.STRING) List<String> textures,
            @ConfigResource(name = "projectileEffectId", defaultValue = "", nullable = true)
            ResourceId projectileEffect
    ) {
        public Config {
            if (attackMultiplier < 1f) {
                throw new IllegalArgumentException("Self attack multiplier must be at least 1");
            }
        }
    }
}
