package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.status.StatusEffectType;

/** 味方へ追加ダメージ倍率を一定時間付与する補助スキル。 */
public final class BonusDamageSupportSkill
        extends AbstractRuntime<BonusDamageSupportSkill.Config> implements SupportAbilityRuntime {
    public BonusDamageSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        SupportSkillStatus.apply(context, context.owner(), StatusEffectType.BONUS_DAMAGE_MULTIPLIER,
                1f + getConfig().magnitude(), getConfig().duration());
        return true;
    }

    /** JSONから読み取る追加ダメージ設定。 */
    public record Config(
            @ConfigFloat(defaultValue = 0f) float magnitude,
            @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
    }
}
