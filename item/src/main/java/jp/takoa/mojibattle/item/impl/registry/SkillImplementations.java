package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.item.impl.ability.StockRiseSkill;
import jp.takoa.mojibattle.registry.AbilityPresentation;
import jp.takoa.mojibattle.registry.SkillImplementation;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 組み込みスキルのFactory、固有検証、ガイド表示定義を実装側へ集約する。
 */
public final class SkillImplementations {
    private static final float DAMAGE_SCALE = .55f;

    private SkillImplementations() {
    }

    public static SkillImplementation projectile() {
        return create("projectile", SkillImplementations::validateProjectile, SkillImplementations::projectilePresentation);
    }

    public static SkillImplementation bindFollowUp() {
        return create("bind_follow_up", config -> {
            validateProjectile(config);
            require(config.glyphs.length >= 2 && config.damages != null && config.bindDurations != null && config.followUpTexture != null && config.followUpDamage > 0 && config.followUpRadius > 0 && config.followUpSpeed > 0, "Bind follow-up skill config is invalid: " + config.id);
        }, (config, multiplier) -> AbilityPresentation.of("guide.bind_follow_up", decimal(config.cooldownSeconds * multiplier), Math.round(config.damages[0] * DAMAGE_SCALE), decimal(config.bindDurations[0]), Math.round(config.followUpDamage * DAMAGE_SCALE), Math.round(points(config))));
    }

    public static SkillImplementation selfAttackBuff() {
        return create("self_attack_buff", config -> {
            validateProjectile(config);
            require(config.buffAttackMultiplier >= 1 && config.buffDurationSeconds > 0, "Self-buff skill config is invalid: " + config.id);
        }, (config, multiplier) -> AbilityPresentation.of("guide.attack_buff", decimal(config.cooldownSeconds * multiplier), Math.round((config.buffAttackMultiplier - 1) * 100), decimal(config.buffDurationSeconds)));
    }

    public static SkillImplementation randomEconomy() {
        return create("random_economy", config -> {
            validateProjectile(config);
            require(config.buffAttackMultiplier > 1 && config.buffDefenseMultiplier > 0 && config.buffDefenseMultiplier < 1 && config.debuffAttackMultiplier > 0 && config.debuffAttackMultiplier < 1 && config.debuffDefenseMultiplier > 1 && config.buffDurationSeconds > 0, "Random-economy skill config is invalid: " + config.id);
        }, (config, multiplier) -> AbilityPresentation.of("guide.random_economy", decimal(config.cooldownSeconds * multiplier), Math.round(points(config))));
    }

    private static SkillImplementation create(String id, Consumer<AbilityConfigs.ProjectileSkill> validator, BiFunction<AbilityConfigs.ProjectileSkill, Float, AbilityPresentation> presentation) {
        return new SkillImplementation(id, StockRiseSkill::new, validator, presentation);
    }

    private static void validateProjectile(AbilityConfigs.ProjectileSkill config) {
        require(config.cooldownSeconds >= 0 && config.projectileLifetimeSeconds > 0 && config.projectileSpeed > 0 && config.glyphs != null && config.textures != null && config.offsets != null && config.glyphs.length > 0 && config.glyphs.length == config.textures.length && config.glyphs.length == config.offsets.length, "Projectile skill config is invalid: " + config.id);
        if (config.damages != null) {
            require(config.damages.length == config.glyphs.length, "Skill damage array length mismatch: " + config.id);
        }
        if (config.bindDurations != null) {
            require(config.bindDurations.length == config.glyphs.length, "Skill bind array length mismatch: " + config.id);
        }
        require(config.healthCostRatio >= 0 && config.healthCostRatio < 1, "Skill health cost is invalid: " + config.id);
    }

    private static AbilityPresentation projectilePresentation(AbilityConfigs.ProjectileSkill config, float cooldownMultiplier) {
        String stun = config.stunSeconds > 0 ? "guide.stun" : "guide.no_stun";
        Object stunArgument = config.stunSeconds > 0 ? decimal(config.stunSeconds) : "";
        AbilityPresentation result = AbilityPresentation.of("guide.skill_stats", decimal(config.cooldownSeconds * cooldownMultiplier), Math.round(config.baseDamage * DAMAGE_SCALE), config.glyphs.length, decimal(Math.max(3.2f, config.projectileRadius * 5f) * 2f), Math.round(points(config)), new AbilityPresentation.Line(stun, new Object[]{stunArgument}));
        if (config.fallingObjectTexture != null) {
            result = result.append("guide.falling_skill", Math.round(config.fallingDamage * DAMAGE_SCALE), decimal(config.fallingRadius * 5f));
        }
        if (config.healthCostRatio > 0) {
            result = result.append("guide.health_cost", Math.round(config.healthCostRatio * 100));
        }
        return result;
    }

    private static float points(AbilityConfigs.ProjectileSkill config) {
        int count = "bind_follow_up".equals(config.implementationId) ? 1 : config.glyphs.length;
        return config.ultimatePointsPerHit * (config.pointsOnHit ? 1 : count);
    }

    private static String decimal(float value) {
        return String.format("%.1f", value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
