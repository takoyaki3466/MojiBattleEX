package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class UltimateStrike implements CombatObject {
    public enum Phase { WARNING, IMPACT, FINISHED }
    public final PlayerId owner;
    public final float radius;
    public final float baseDamage;
    public final float spawnHeight;
    public final int warningEffectIndex;
    public final int impactEffectIndex;
    public final String dropStyle;
    public final String objectTexture;
    public final float fallHorizontalOffset;
    public final float burnDuration,burnDamage,burnTickInterval,burnUltimatePointsPerTick;
    private final Vector3 position = new Vector3();
    private final float warningDuration;
    private final float effectDuration;
    private float elapsed;
    private Phase phase = Phase.WARNING;
    private boolean damagePending;

    public UltimateStrike(PlayerId owner, Vector3 lockedTarget, float radius, float damage, float warning, float effect, float spawnHeight,
                          int warningEffectIndex, int impactEffectIndex, String dropStyle, String objectTexture, float fallHorizontalOffset) {
        this(owner,lockedTarget,radius,damage,warning,effect,spawnHeight,warningEffectIndex,impactEffectIndex,dropStyle,objectTexture,fallHorizontalOffset,0,0,1,0);
    }
    public UltimateStrike(PlayerId owner,Vector3 lockedTarget,float radius,float damage,float warning,float effect,float spawnHeight,
                          int warningEffectIndex,int impactEffectIndex,String dropStyle,String objectTexture,float fallHorizontalOffset,float burnDuration,float burnDamage,float burnTickInterval,float burnUltimatePointsPerTick){
        this.owner = owner; this.position.set(lockedTarget.x, 0, lockedTarget.z); this.radius = radius;
        this.baseDamage = damage; this.warningDuration = warning; this.effectDuration = effect; this.spawnHeight = spawnHeight;
        this.warningEffectIndex = warningEffectIndex; this.impactEffectIndex = impactEffectIndex; this.dropStyle = dropStyle;
        this.objectTexture = objectTexture; this.fallHorizontalOffset = fallHorizontalOffset;this.burnDuration=burnDuration;this.burnDamage=burnDamage;this.burnTickInterval=burnTickInterval;this.burnUltimatePointsPerTick=burnUltimatePointsPerTick;
    }
    public void update(float delta) {
        elapsed += delta;
        if (phase == Phase.WARNING && elapsed >= warningDuration) { phase = Phase.IMPACT; damagePending = true; }
        if (phase == Phase.IMPACT && elapsed >= warningDuration + effectDuration) phase = Phase.FINISHED;
    }
    public boolean takeDamagePending() { boolean result = damagePending; damagePending = false; return result; }
    public float fallProgress() { return warningDuration <= 0 ? 1 : Math.min(1, elapsed / warningDuration); }
    public Phase phase() { return phase; }
    public boolean expired() { return phase == Phase.FINISHED; }
    public Vector3 position() { return position; }
}
