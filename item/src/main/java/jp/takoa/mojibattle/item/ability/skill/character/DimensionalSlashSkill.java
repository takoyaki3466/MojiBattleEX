package jp.takoa.mojibattle.item.ability.skill.character;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.action.spec.BattlefieldPulseSpec;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

/** 斬撃パルスを生成する「次元斬」スキル。 */
public final class DimensionalSlashSkill extends AbstractRuntime<DimensionalSlashSkill.Config>
        implements SkillRuntime {
    public DimensionalSlashSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        context.actions().startBattlefieldPulse(context.user(), new BattlefieldPulseSpec(
                4.2f, .42f, 5.8f, DamageRequest.of(9f, DamageType.SKILL),
                new ResourceId("mojibattle", "unit"), 9f));
        context.actions().addUltimatePoints(context.user(), 18f);
        return true;
    }

    /** 固有設定を持たないことを表す設定record。 */
    public record Config() {
    }
}
