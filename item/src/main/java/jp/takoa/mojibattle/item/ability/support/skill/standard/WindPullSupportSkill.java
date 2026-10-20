package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;

/** 敵を引き寄せて拘束する補助スキル。 */
public final class WindPullSupportSkill
        extends AbstractRuntime<WindPullSupportSkill.Config> implements SupportAbilityRuntime {
    public WindPullSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
            context.actions().pullToward(enemy, context.owner(), getConfig().range(), 2f);
            context.actions().bind(enemy, getConfig().duration());
        }
        return true;
    }

    /** JSONから読み取る引き寄せ設定。 */
    public record Config(
            @ConfigFloat(defaultValue = 0f) float range,
            @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
    }
}
