package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/** 警告、着弾、終了の段階を持つ落下型必殺攻撃の戦場オブジェクト。 */
public final class UltimateStrike extends PositionedCombatObject {
    /** 落下攻撃の警告表示から終了までの進行段階。 */
    public enum Phase {
        PENDING,
        WARNING,
        IMPACT,
        FINISHED
    }
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
    private final float warningDuration;
    private final float effectDuration;
    private float elapsed;
    private Phase phase = Phase.WARNING;
    private boolean damagePending;
    private boolean mainUltimate = true;

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
        if (phase == Phase.PENDING && elapsed >= 0) phase = Phase.WARNING;
        if (phase == Phase.WARNING && elapsed >= warningDuration) { phase = Phase.IMPACT; damagePending = true; }
        if (phase == Phase.IMPACT && elapsed >= warningDuration + effectDuration) phase = Phase.FINISHED;
    }
    /** 指定秒数が経過するまで警告・落下演出を開始しない。 */
    public UltimateStrike startAfter(float seconds) {
        elapsed = -Math.max(0, seconds);
        phase = elapsed < 0 ? Phase.PENDING : Phase.WARNING;
        return this;
    }
    /** 通常スキル由来として必殺技専用倍率の適用対象から外す。 */
    public UltimateStrike skillAttack() { mainUltimate = false; return this; }
    public boolean isMainUltimate() { return mainUltimate; }
    public boolean takeDamagePending() { boolean result = damagePending; damagePending = false; return result; }
    public float fallProgress() { return warningDuration <= 0 ? 1 : Math.min(1, elapsed / warningDuration); }
    public Phase phase() { return phase; }
    public boolean expired() { return phase == Phase.FINISHED; }
}
