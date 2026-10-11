package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;
import java.util.*;
import java.util.function.BiConsumer;

/** Data-driven support actions; character IDs never participate in dispatch. */
public final class SupportAbilityService {
    private record Context(BattleController battle,PlayerTeam team,SupportAbilityDefinition ability){}
    private final Map<String,BiConsumer<Context,Boolean>> handlers=new HashMap<>();
    public SupportAbilityService(){
        handlers.put("ally_attack_up",(c,u)->{c.team.main().statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.ATTACK_UP,1+c.ability.magnitude,c.ability.durationSeconds));c.battle.emitImageEffect(c.team.main().position,"glyphs/t.png",14,8,1.1f);});
        handlers.put("enemy_defense_down",(c,u)->{for(CharacterRuntime target:enemies(c)){target.statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.DAMAGE_TAKEN_UP,1+c.ability.magnitude,c.ability.durationSeconds));Vector3 side=new Vector3(-target.facing.z,0,target.facing.x);c.battle.emitEffect(new Vector3(target.position).mulAdd(side,3),14,9,1.5f);c.battle.emitEffect(new Vector3(target.position).mulAdd(side,-3),14,9,1.5f);}});
        handlers.put("enemy_attack_down",(c,u)->{for(CharacterRuntime target:enemies(c)){target.statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.ATTACK_DOWN,1-c.ability.magnitude,c.ability.durationSeconds));c.battle.emitImageEffect(target.position,"glyphs/zero.png",5,7,1.2f);}});
        handlers.put("heal_cleanse",(c,u)->{c.team.main().heal(c.team.main().definition.maxHp*c.ability.magnitude);c.team.main().clearNegativeStatuses();c.battle.emitImageEffect(c.team.main().position,"glyphs/zero.png",5,11,1.5f);});
        handlers.put("bonus_damage",(c,u)->{c.team.main().statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.BONUS_DAMAGE,1+c.ability.magnitude,c.ability.durationSeconds));c.battle.emitImageEffect(c.team.main().position,"glyphs/slash.png",12,9,1.1f);});
        handlers.put("wide_slash",(c,u)->c.battle.spawn(new LineStrike(c.team.main().playerId,c.team.support().position,new Vector3(enemy(c).position).sub(c.team.support().position),c.ability.range,3.5f,c.ability.baseDamage,.25f,.8f,3,12,"objects/energy-lance.png")));
        handlers.put("enemy_slow",(c,u)->{for(CharacterRuntime target:enemies(c)){target.statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.MOVE_DOWN,1-c.ability.magnitude,c.ability.durationSeconds));c.battle.emitEffect(target.position,3,6,1);}});
        handlers.put("right_teleport",(c,u)->{if(c.battle.teleportRight(c.team.main().playerId,c.ability.range))c.team.main().statuses.apply(new StatusEffect(c.ability.id,c.team.main().playerId,StatusType.ATTACK_UP,1+c.ability.magnitude,c.ability.durationSeconds));else c.team.support().addUltimatePoints(c.ability.pointCost);});
        handlers.put("wind_pull",(c,u)->{for(CharacterRuntime target:enemies(c)){Vector3 direction=new Vector3(c.team.main().position).sub(target.position);direction.y=0;float distance=direction.len();if(distance>0)target.position.mulAdd(direction.scl(1f/distance),Math.min(c.ability.range,Math.max(0,distance-2f)));target.applyBind(c.ability.durationSeconds);c.battle.emitImageEffect(target.position,"glyphs/matsu.png",11,9,1.25f);}});
        handlers.put("global_cooldown_halve",(c,u)->{c.battle.halveAllSkillCooldowns();for(CharacterRuntime target:c.battle.players())c.battle.emitImageEffect(target.position,"glyphs/matsu.png",15,10,1.5f);});
    }
    public void useSkill(BattleController battle,PlayerTeam team){var s=team.support();var a=s.definition.skill;if(!s.skillReady()||team.main().isStunned())return;s.startSkillCooldown(battle.supportSkillCooldown(a.cooldownSeconds));execute(battle,team,a,false);}
    public void useUltimate(BattleController battle,PlayerTeam team){var a=team.support().definition.ultimate;if(team.main().isStunned()||!team.support().spendUltimate(a.pointCost))return;execute(battle,team,a,true);battle.announceUltimate(team.main().playerId,a.nameKey);}
    private void execute(BattleController battle,PlayerTeam team,SupportAbilityDefinition ability,boolean ultimate){var handler=handlers.get(ability.type);if(handler==null)throw new IllegalArgumentException("Unknown support ability: "+ability.type);handler.accept(new Context(battle,team,ability),ultimate);for(int ring=0;ring<(ultimate?4:2);ring++)battle.emitEffect(team.support().position,(ultimate?14:6)+ring,ultimate?12-ring*1.6f:7-ring,ultimate?1.4f:.8f);}
    private static CharacterRuntime enemy(Context c){return c.battle.targetOf(c.team.main().playerId);}
    private static List<CharacterRuntime> enemies(Context c){return c.battle.players().stream().filter(target->target.playerId!=c.team.main().playerId&&target.isAlive()).toList();}
}
