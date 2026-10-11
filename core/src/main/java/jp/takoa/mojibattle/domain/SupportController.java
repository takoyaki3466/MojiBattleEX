package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class SupportController {
    private final SupportAbilityService abilities=new SupportAbilityService();
    public void update(BattleController battle,SupportCharacterRuntime support,CharacterRuntime ally,CharacterRuntime enemy,float delta){
        support.update(delta);
        if(support.skillReady())abilities.useSkill(battle,battle.team(support.owner));
        float side=support.owner.ordinal()%2==0?-support.definition.followDistance:support.definition.followDistance;
        Vector3 desired=new Vector3(ally.position).mulAdd(new Vector3(-ally.facing.z,0,ally.facing.x),side);
        Vector3 movement=desired.sub(support.position);movement.y=0;
        float distance=movement.len();
        if(distance>.3f)support.position.mulAdd(movement.nor(),Math.min(support.definition.moveSpeed*battle.fieldMoveMultiplier()*delta,distance));
        support.position.y=ally.position.y;
        if(support.attackReady()&&support.position.dst2(enemy.position)<=support.definition.attackRange*support.definition.attackRange){
            Vector3 start=new Vector3(support.position).add(0,1,0);Vector3 velocity=new Vector3(enemy.position).add(0,1,0).sub(start).nor().scl(support.definition.projectileSpeed);
            Projectile shot=new Projectile(battle.nextAttackId(),support.owner,support.definition.displayGlyph,support.definition.projectileTexture,6,start,velocity,.42f,support.definition.baseDamage,0,2.5f)
                .criticalProfile(support.definition.criticalEnabled,support.definition.criticalRate,support.definition.criticalDamageMultiplier);
            if(support.definition.curvedProjectile)shot.curve(24f);
            battle.spawn(shot);support.resetAttack();
        }
    }
    public void useSkill(BattleController battle,PlayerTeam team){abilities.useSkill(battle,team);}
    public void useUltimate(BattleController battle,PlayerTeam team){abilities.useUltimate(battle,team);}
}
