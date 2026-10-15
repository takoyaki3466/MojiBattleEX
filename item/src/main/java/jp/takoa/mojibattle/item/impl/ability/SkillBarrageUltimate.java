package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.SkillBarrage;

/** 指定スキルを設定回数だけ時間差で連続発動する必殺技。 */
public final class SkillBarrageUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public SkillBarrageUltimate(AbilityConfigs.AreaUltimate config) {
        this.config = config;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions()
                || !user.isAlive()
                || user.isStunned()
                || !user.spendUltimate(config.pointCost)) {
            return false;
        }
        battle.spawn(new SkillBarrage(
                user,
                config.barrageSkillId,
                config.barrageCount,
                config.barrageInterval,
                user.position));
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
