package jp.takoa.mojibattle.domain.ability;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.*;

public final class MovingCarUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    public MovingCarUltimate(AbilityConfigs.AreaUltimate config) { this.config = config; }
    @Override public String id() { return config.id; }
    @Override public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || !user.spendUltimate(config.pointCost)) return false;
        int count=Math.max(1,config.summonCount);
        for(int i=0;i<count;i++){
            float angle=-27f+i*18f;
            Vector3 direction=new Vector3(user.facing).rotate(Vector3.Y,angle);
            Vector3 start=new Vector3(user.position).mulAdd(direction,i*.45f);
            battle.spawn(new CarHazard(user.playerId,start,direction,config.movementSpeed,
                config.radius,config.baseDamage,config.stunSeconds,config.effectSeconds,
                battle.arenaHalfExtent(),config.objectTexture,config.impactEffectIndex));
        }
        battle.emitImageEffect(new Vector3(user.position).add(0, 1.5f, 0), config.objectTexture,
            config.impactEffectIndex, 9f, 1.2f);
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
