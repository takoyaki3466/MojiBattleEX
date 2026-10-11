package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.PlayerId;
import java.util.EnumMap;

public final class LongTermInvestmentPassive implements IPassive {
    private final AbilityConfigs.TimedPassive config;
    private final EnumMap<PlayerId,Float> rollTimers=new EnumMap<>(PlayerId.class),cooldowns=new EnumMap<>(PlayerId.class);
    public LongTermInvestmentPassive(AbilityConfigs.TimedPassive config) { this.config = config; }
    public String id() { return config.id; }
    public float attackMultiplier(CharacterRuntime owner, float elapsed, int alliedSummons) {
        if("berserker".equals(config.type))return Math.min(config.maximumMultiplier,1f+(1f-owner.hpRatio())*config.missingHealthAttackBonus);
        int intervals = (int)Math.floor(Math.max(0, elapsed) / config.intervalSeconds);
        return Math.min(config.maximumMultiplier, 1f + intervals * config.attackIncreasePerInterval
            + alliedSummons * config.familyBonusPerMember);
    }
    public float moveSpeedMultiplier(CharacterRuntime owner, float elapsed) { return positiveOrOne(config.moveSpeedMultiplier); }
    public float skillCooldownMultiplier(CharacterRuntime owner) { return positiveOrOne(config.skillCooldownMultiplier); }
    public float ultimateGainMultiplier(CharacterRuntime owner) { return positiveOrOne(config.ultimateGainMultiplier); }
    public float incomingDamageMultiplier(CharacterRuntime owner) { return positiveOrOne(config.incomingDamageMultiplier); }
    public float hitboxMultiplier(CharacterRuntime owner) { return positiveOrOne(config.hitboxMultiplier); }
    public float stunMultiplier(CharacterRuntime owner) { return positiveOrOne(config.stunMultiplier); }
    public void update(BattleController battle,CharacterRuntime owner, float delta) {
        if (config.ultimatePointsPerSecond > 0) battle.addSkillUltimatePoints(owner,config.ultimatePointsPerSecond * Math.max(0, delta));
        if(!"difficult_kanji".equals(config.type))return;
        float cooldown=Math.max(0,cooldowns.getOrDefault(owner.playerId,0f)-delta);cooldowns.put(owner.playerId,cooldown);
        float timer=rollTimers.getOrDefault(owner.playerId,config.intervalSeconds)-delta;if(timer>0){rollTimers.put(owner.playerId,timer);return;}rollTimers.put(owner.playerId,timer+config.intervalSeconds);
        long interval=(long)Math.floor(battle.timer().elapsed()/config.intervalSeconds);long roll=Math.floorMod(interval*3701+owner.playerId.ordinal()*1709,10000);
        if(cooldown<=0&&roll<config.procChance*10000f){CharacterRuntime target=battle.targetOf(owner.playerId);if(target!=null)target.applyStun(config.procStunSeconds);cooldowns.put(owner.playerId,config.procInternalCooldown);}
    }
    private static float positiveOrOne(float value) { return value > 0 ? value : 1f; }
}
