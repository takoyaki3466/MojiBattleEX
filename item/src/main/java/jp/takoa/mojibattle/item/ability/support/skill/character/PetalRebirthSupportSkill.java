package jp.takoa.mojibattle.item.ability.support.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.status.StatusDuration;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.battle.status.StatusStacking;
import jp.takoa.mojibattle.resource.ResourceId;

/** 味方へ継続回復を付与する補助スキル。 */
public final class PetalRebirthSupportSkill
        extends AbstractRuntime<PetalRebirthSupportSkill.Config> implements SupportAbilityRuntime {
    public PetalRebirthSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        for (CombatantView ally : context.actions().alliesOf(context.owner())) {
            context.actions().applyStatus(context.owner(), ally, new StatusSpec(
                    new ResourceId("mojibattle", "petal_rebirth_hot"), StatusEffectType.HEAL_OVER_TIME,
                    15f, StatusDuration.seconds(7f), StatusStacking.ADD_STACK, 3, 1f));
        }
        return true;
    }

    /** 固有設定を持たないことを表す設定record。 */
    public record Config() {
    }
}
