package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.*;

public final class SkillBarrageUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    public SkillBarrageUltimate(AbilityConfigs.AreaUltimate config){this.config=config;}
    public String id(){return config.id;}
    public boolean tryActivate(BattleController battle,CharacterRuntime user){
        if(!battle.acceptsActions()||!user.isAlive()||user.isStunned()||!user.spendUltimate(config.pointCost))return false;
        battle.spawn(new SkillBarrage(user.playerId,config.barrageSkillId,config.barrageCount,config.barrageInterval,user.position));
        battle.announceUltimate(user.playerId,config.nameKey);return true;
    }
}
