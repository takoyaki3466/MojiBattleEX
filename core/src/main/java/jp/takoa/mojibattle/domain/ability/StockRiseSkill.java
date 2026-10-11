package jp.takoa.mojibattle.domain.ability;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.Projectile;

public final class StockRiseSkill implements ISkill {
    private static final float GLOBAL_SKILL_DAMAGE_SCALE = .55f;
    private final AbilityConfigs.ProjectileSkill config;
    public StockRiseSkill(AbilityConfigs.ProjectileSkill config) { this.config = config; }
    public String id() { return config.id; }
    @Override public void initialize(CharacterRuntime user){if(config.maximumStocks>1)user.configureSkillStocks(config.maximumStocks,config.initialStocks,config.stockRechargeSeconds);}
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || !user.skillReady()) return false;
        float healthCost=user.definition.maxHp*Math.max(0,config.healthCostRatio);
        if(healthCost>0&&!user.spendHealth(healthCost))return false;
        if(config.maximumStocks>1&&!user.consumeSkillStock())return false;
        fire(battle,user,true); return true;
    }
    @Override public void activateFromUltimate(BattleController battle,CharacterRuntime user){fire(battle,user,false);}
    private void fire(BattleController battle,CharacterRuntime user,boolean consumeResources){
        int projectileCount="bind_follow_up".equals(config.type)?1:config.glyphs.length;
        if(consumeResources){user.startSkillCooldown(battle.skillCooldown(user,config.cooldownSeconds));if(!config.pointsOnHit)battle.addSkillUltimatePoints(user, config.ultimatePointsPerHit * projectileCount);}
        battle.emitEffect(new Vector3(user.position).add(0, 1, 0), config.effectIndex, 2.8f, .32f);
        long attackId = battle.nextAttackId();
        Vector3 forward = new Vector3(user.facing).nor();
        // Keep projectile formations aligned with the same screen-right basis used by movement.
        Vector3 right = new Vector3(-forward.z, 0, forward.x);
        for (int i = 0; i < projectileCount; i++) {
            float[] offset = config.offsets[i];
            Vector3 shotDirection = config.radial
                ? new Vector3(MathUtils.sin(MathUtils.PI2 * i / config.glyphs.length), 0, MathUtils.cos(MathUtils.PI2 * i / config.glyphs.length))
                : new Vector3(forward);
            Vector3 start = new Vector3(user.position).mulAdd(shotDirection, 1.0f);
            if (!config.radial) start.mulAdd(right, offset[0]);
            start.y += 1.1f + offset[1];
            float damage=config.damages!=null&&i<config.damages.length?config.damages[i]:config.baseDamage;
            float bind=config.bindDurations!=null&&i<config.bindDurations.length?config.bindDurations[i]:config.bindSeconds;
            Projectile projectile=new Projectile(attackId,user.playerId,config.glyphs[i],config.textures[i],config.effectIndex,start,
                shotDirection.scl(config.projectileSpeed*1.3f),config.projectileRadius,damage*GLOBAL_SKILL_DAMAGE_SCALE,
                config.stunSeconds,bind,config.projectileLifetimeSeconds,config.boomerang,config.boomerangDistance,
                config.pointsOnHit?config.ultimatePointsPerHit:0);
            if("bind_follow_up".equals(config.type))projectile.followUp(config.glyphs[1],config.followUpTexture,
                config.followUpDamage*GLOBAL_SKILL_DAMAGE_SCALE,config.followUpRadius,config.followUpSpeed);
            battle.spawn(projectile);
        }
    }
}
