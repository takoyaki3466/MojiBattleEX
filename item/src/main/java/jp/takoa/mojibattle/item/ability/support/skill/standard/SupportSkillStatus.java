package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.status.StatusDuration;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.resource.ResourceId;

/** 標準補助スキルの状態付与処理を共有する。 */
final class SupportSkillStatus {
    private SupportSkillStatus() {
    }

    static void apply(SupportAbilityActivationContext context, CombatantView target,
                      StatusEffectType type, float value, float seconds) {
        context.actions().applyStatus(context.owner(), target, StatusSpec.multiplier(
                new ResourceId("mojibattle", "support/" + type.name().toLowerCase(java.util.Locale.ROOT)),
                type, Math.max(0f, value), StatusDuration.seconds(seconds)));
    }

    static void applyEnemies(SupportAbilityActivationContext context, StatusEffectType type,
                             float value, float seconds) {
        for (CombatantView target : context.actions().enemiesOf(context.owner())) {
            apply(context, target, type, value, seconds);
        }
    }
}
