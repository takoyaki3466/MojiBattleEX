package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.status.StatusEffectType;

/**
 * 敵の攻撃倍率を一定時間低下させる補助スキル。
 */
public final class EnemyAttackDownSupportSkill extends AbstractRuntime<EnemyAttackDownSupportSkill.Config> implements SupportAbilityRuntime {
    public EnemyAttackDownSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        SupportSkillStatus.applyEnemies(context, StatusEffectType.ATTACK_MULTIPLIER, Math.max(0f, 1f - getConfig().magnitude()), getConfig().duration());
        return true;
    }

    /**
     * JSONから読み取る攻撃低下設定。
     */
    public record Config(
            @ConfigFloat(defaultValue = 0f) float magnitude,
            @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
    }
}
