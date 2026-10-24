package jp.takoa.mojibattle.item.ability.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * 周囲を攻撃して押し戻し、自身を回復する捕食スキル。
 */
public final class PredatoryPounceSkill extends AbstractRuntime<PredatoryPounceSkill.Config> implements SkillRuntime {
    public PredatoryPounceSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        List<CombatantView> enemies = context.actions().enemiesOf(context.user());
        for (CombatantView enemy : enemies) {
            context.actions().damage(context.user(), enemy, DamageRequest.of(42f, DamageType.SKILL));
            context.actions().knockbackAwayFrom(context.user(), enemy, 2.2f);
        }
        context.actions().healRatio(context.user(), .045f * Math.max(1, enemies.size()));
        context.actions().addUltimatePoints(context.user(), 16f);
        context.actions().emitEffect(context.user(), new ResourceId("mojibattle", "fire"), 8f, .65f);
        return true;
    }

    /**
     * 固有設定を持たないことを表す設定record。
     */
    public record Config() {
    }
}
