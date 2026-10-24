package jp.takoa.mojibattle.item.ability.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.action.spec.ProjectileSpec;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

/**
 * 対象と位置を交換し、防御を無視する横方向の弾幕を生成するスキル。
 */
public final class SidewaysRaidSkill extends AbstractRuntime<SidewaysRaidSkill.Config> implements SkillRuntime {
    public SidewaysRaidSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        if (context.target() != null) {
            context.actions().swapPositions(context.user(), context.target());
        }
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().spawnProjectile(context.user(), enemy, new ProjectileSpec(new DamageRequest(26f, DamageType.SKILL, false, true), 25f, .9f, 2.6f, 4, 46f, 1, new ResourceId("mojibattle", "glyphs/crab.png"), new ResourceId("mojibattle", "impact")));
        }
        context.actions().addUltimatePoints(context.user(), 17f);
        return true;
    }

    /**
     * 固有設定を持たないことを表す設定record。
     */
    public record Config() {
    }
}
