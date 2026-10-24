package jp.takoa.mojibattle.item.ability.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigString;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.status.StatusEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 敵味方へ独立した好況・不況補正を抽選するスキル。
 */
public final class RandomEconomySkill extends AbstractRuntime<RandomEconomySkill.Config> implements SkillRuntime {
    public RandomEconomySkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        roll(context, context.user());

        for (CombatantView target : context.actions().statusEffectEnemiesOf(context.user())) {
            roll(context, target);
        }

        context.actions().addUltimatePoints(context.user(), getConfig().ultimatePoints());
        return true;
    }

    private void roll(AbilityActivationContext context, CombatantView target) {
        Config config = getConfig();

        boolean boom = ThreadLocalRandom.current().nextBoolean();

        context.actions().applyStatus(context.user(), target, StandardProjectileSupport.status(config.id() + "/economy/attack", StatusEffectType.ATTACK_MULTIPLIER, boom ? config.buffAttackMultiplier() : config.debuffAttackMultiplier(), config.durationSeconds()));
        context.actions().applyStatus(context.user(), target, StandardProjectileSupport.status(config.id() + "/economy/defense", StatusEffectType.INCOMING_DAMAGE_MULTIPLIER, boom ? config.buffDefenseMultiplier() : config.debuffDefenseMultiplier(), config.durationSeconds()));
    }

    /**
     * JSONから読み取る好況・不況スキル設定。
     */
    public record Config(
            @RequiredConfig @ConfigString(defaultValue = "ability") String id,
            @ConfigFloat(defaultValue = 1f) float buffAttackMultiplier,
            @ConfigFloat(defaultValue = 1f) float buffDefenseMultiplier,
            @ConfigFloat(defaultValue = 1f) float debuffAttackMultiplier,
            @ConfigFloat(defaultValue = 1f) float debuffDefenseMultiplier,
            @ConfigFloat(name = "buffDurationSeconds", defaultValue = 1f, exclusiveMin = true) float durationSeconds,
            @ConfigFloat(name = "ultimatePointsPerHit", defaultValue = 0f) float ultimatePoints) {

        public Config {
            if (buffAttackMultiplier <= 1f || buffDefenseMultiplier <= 0f || buffDefenseMultiplier >= 1f || debuffAttackMultiplier <= 0f || debuffAttackMultiplier >= 1f || debuffDefenseMultiplier <= 1f) {
                throw new IllegalArgumentException("Random economy multipliers are invalid");
            }
        }
    }
}
