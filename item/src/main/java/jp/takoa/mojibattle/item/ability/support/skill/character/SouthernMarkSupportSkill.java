package jp.takoa.mojibattle.item.ability.support.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.status.StatusConsumption;
import jp.takoa.mojibattle.battle.status.StatusDuration;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.battle.status.StatusStacking;
import jp.takoa.mojibattle.resource.ResourceId;

/** 敵へ次回被ダメージ増加の印を付与する補助スキル。 */
public final class SouthernMarkSupportSkill
        extends AbstractRuntime<SouthernMarkSupportSkill.Config> implements SupportAbilityRuntime {
    public SouthernMarkSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        for (CombatantView enemy : context.actions().statusEffectEnemiesOf(context.owner())) {
            context.actions().applyStatus(context.owner(), enemy, new StatusSpec(
                    id("southern_mark_status"), StatusEffectType.NEXT_INCOMING_DAMAGE_MULTIPLIER,
                    1.65f, StatusDuration.seconds(8f), StatusStacking.REFRESH, 1, 1f,
                    StatusConsumption.ON_DAMAGE_RECEIVED));
        }
        context.actions().emitEffect(context.owner(), id("wind"), 10f, .75f);
        return true;
    }

    /** 固有設定を持たないことを表す設定record。 */
    public record Config() {
    }

    private static ResourceId id(String path) {
        return new ResourceId("mojibattle", path);
    }
}
