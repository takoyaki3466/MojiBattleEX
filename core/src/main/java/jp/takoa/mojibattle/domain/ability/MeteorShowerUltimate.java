package jp.takoa.mojibattle.domain.ability;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.*;

import java.util.Random;

public final class MeteorShowerUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    public MeteorShowerUltimate(AbilityConfigs.AreaUltimate config){this.config=config;}
    @Override public String id(){return config.id;}
    @Override public boolean tryActivate(BattleController battle, CharacterRuntime user){
        if(!battle.acceptsActions()||!user.isAlive()||user.isStunned()||!user.spendUltimate(config.pointCost))return false;
        Random random=new Random(battle.nextAttackId());
        float extent=battle.arenaHalfExtent()*.9f;
        CharacterRuntime targetPlayer=battle.targetOf(user.playerId);if(targetPlayer==null)return false;
        for(int i=0;i<config.meteorCount;i++){
            boolean arenaWide="meteor".equals(config.dropStyle);
            float spread=arenaWide?extent:Math.max(1.8f,config.radius*.65f);
            Vector3 target=arenaWide
                ? new Vector3((random.nextFloat()*2-1)*extent,0,(random.nextFloat()*2-1)*extent)
                : new Vector3(targetPlayer.position).add((random.nextFloat()*2-1)*spread,0,(random.nextFloat()*2-1)*spread);
            float warning=config.warningSeconds+(i%5)*.16f;
            battle.spawn(new UltimateStrike(user.playerId,target,config.radius,config.baseDamage,warning,config.effectSeconds,
                config.spawnHeight,config.warningEffectIndex,config.impactEffectIndex,config.dropStyle,config.objectTexture,config.fallHorizontalOffset));
        }
        battle.announceUltimate(user.playerId,config.nameKey);
        return true;
    }
}
