package jp.takoa.mojibattle.item.ability.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

/**
 * 対象と位置を交換して敵全体を攻撃するスキル。
 */
public final class EastExchangeSkill extends AbstractRuntime<EastExchangeSkill.Config> implements SkillRuntime {
    public EastExchangeSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        if (context.target() != null) {
            context.actions().swapPositions(context.user(), context.target());
        }
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().damage(context.user(), enemy, DamageRequest.of(31f, DamageType.SKILL));
        }
        context.actions().addUltimatePoints(context.user(), 22f);
        context.actions().emitEffect(context.user(), new ResourceId("mojibattle", "star"), 8f, .5f);
        return true;
    }

    /**
     * 固有設定を持たないことを表す設定record。
     */
    public record Config() {
    }
}
