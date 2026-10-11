package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class CharacterRuntime {
    public final PlayerId playerId;
    public final CharacterDefinition definition;
    public final Vector3 position = new Vector3();
    public final Vector3 facing = new Vector3(0, 0, 1);
    private float hp;
    private float skillCooldown;
    private float skillCooldownDuration;
    private float ultimatePoints;
    private float totalDamage;
    private boolean immortal;
    private float stunRemaining;
    private float bindRemaining;
    private int skillStocks = 1, maximumSkillStocks = 1;
    private float stockRechargeSeconds, stockRechargeRemaining;
    private boolean awakened;
    public final StatusEffectSystem statuses;

    public CharacterRuntime(PlayerId playerId, CharacterDefinition definition, Vector3 spawn) {
        this.playerId = playerId;
        this.definition = definition;
        statuses=new StatusEffectSystem(playerId);
        position.set(spawn);
        hp = definition.maxHp;
    }

    public void update(float delta) {
        skillCooldown = Math.max(0, skillCooldown - delta);
        stunRemaining = Math.max(0, stunRemaining - delta);
        bindRemaining = Math.max(0, bindRemaining - delta);
        statuses.update(delta);
        if (skillStocks < maximumSkillStocks && stockRechargeSeconds > 0) {
            stockRechargeRemaining -= delta;
            if (stockRechargeRemaining <= 0) { skillStocks++; stockRechargeRemaining = stockRechargeSeconds; }
        }
    }
    public boolean isAlive() { return immortal || hp > 0; }
    public float hp() { return hp; }
    public float hpRatio() { return hp / definition.maxHp; }
    public float receiveDamage(float damage) {
        if (immortal) return Math.max(0, damage);
        if (!isAlive()) return 0;
        float before = hp;
        hp = Math.max(0, hp - damage);
        return before - hp;
    }
    public float skillCooldown() { return skillCooldown; }
    public boolean skillReady() { return skillCooldown <= 0&&skillStocks>0; }
    public void startSkillCooldown(float seconds) { skillCooldownDuration=Math.max(0,seconds);skillCooldown=skillCooldownDuration; }
    public void resetCooldown() { skillCooldown = 0; }
    public void scaleSkillCooldown(float multiplier){float safe=Math.max(0,multiplier);skillCooldown=Math.max(0,skillCooldown*safe);stockRechargeRemaining=Math.max(0,stockRechargeRemaining*safe);}
    public float skillChargeRatio(){
        if(skillStocks<=0&&stockRechargeSeconds>0)return Math.max(0,Math.min(1,1-stockRechargeRemaining/stockRechargeSeconds));
        return skillCooldownDuration<=0?1f:Math.max(0,Math.min(1,1-skillCooldown/skillCooldownDuration));
    }
    public float ultimatePoints() { return ultimatePoints; }
    public void addUltimatePoints(float points) { ultimatePoints = Math.min(100, Math.max(0, ultimatePoints + points)); }
    public boolean spendUltimate(float points) {
        if (points < 0 || ultimatePoints < points) return false;
        ultimatePoints -= points;
        return true;
    }
    public void maximizeUltimate() { ultimatePoints = 100; }
    public float totalDamage() { return totalDamage; }
    public void recordDamage(float damage) { totalDamage += Math.max(0, damage); }
    public void resetDamage() { totalDamage = 0; }
    public void setImmortal(boolean value) { immortal = value; }
    public void heal(float amount) { if (isAlive() && Float.isFinite(amount) && amount > 0) hp = Math.min(definition.maxHp, hp + amount); }
    public boolean spendHealth(float amount){if(!isAlive()||!Float.isFinite(amount)||amount<=0||hp<=amount)return false;hp-=amount;return true;}
    public void applyStun(float seconds) { if (isAlive() && Float.isFinite(seconds)&&seconds>0){stunRemaining = Math.max(stunRemaining, seconds);statuses.apply(new StatusEffect("stun",playerId.opponent(),StatusType.STUN,1,seconds));} }
    public boolean isStunned() { return stunRemaining > 0; }
    public float stunRemaining() { return stunRemaining; }
    public void applyBind(float seconds) { if(Float.isFinite(seconds)&&seconds>0){bindRemaining=Math.max(bindRemaining,seconds);statuses.apply(new StatusEffect("bind",playerId.opponent(),StatusType.BIND,1,seconds));} }
    public boolean movementLocked() { return isStunned() || bindRemaining > 0; }
    public void configureSkillStocks(int maximum,int initial,float recharge){maximumSkillStocks=Math.max(1,maximum);skillStocks=Math.max(0,Math.min(maximumSkillStocks,initial));stockRechargeSeconds=Math.max(0,recharge);stockRechargeRemaining=stockRechargeSeconds;}
    public boolean consumeSkillStock(){if(skillStocks<=0)return false;skillStocks--;if(skillStocks<maximumSkillStocks&&stockRechargeRemaining<=0)stockRechargeRemaining=stockRechargeSeconds;return true;}
    public int skillStocks(){return skillStocks;}
    public int maximumSkillStocks(){return maximumSkillStocks;}
    public void awaken(){awakened=true;}
    public boolean awakened(){return awakened;}
    public void clearNegativeStatuses(){stunRemaining=0;bindRemaining=0;statuses.clearNegative();}
}
