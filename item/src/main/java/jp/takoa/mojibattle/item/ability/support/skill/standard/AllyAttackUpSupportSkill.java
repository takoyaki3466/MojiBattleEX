package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.status.StatusEffectType;

/** 味方の攻撃倍率を一定時間増加させる補助スキル。 */
public final class AllyAttackUpSupportSkill
        extends AbstractRuntime<AllyAttackUpSupportSkill.Config> implements SupportAbilityRuntime {
    public AllyAttackUpSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        SupportSkillStatus.apply(context, context.owner(), StatusEffectType.ATTACK_MULTIPLIER,
                1f + getConfig().magnitude(), getConfig().duration());
        return true;
    }

    /** JSONから読み取る攻撃強化設定。 */
    public record Config(
            @ConfigFloat(defaultValue = 0f) float magnitude,
            @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
    }
}
