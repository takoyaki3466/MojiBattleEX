package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

import java.util.EnumSet;

/** 巨大な「1」を回転させ、各周回で全長に触れたすべての敵を攻撃する判定オブジェクト。 */
public final class SpinningOne extends PositionedCombatObject {
    public final PlayerId owner;
    public final float length, width, baseDamage, healOnHit;
    public final String texturePath;
    public final int effectIndex;
    private float angle;
    private float remainingLife;
    private float hitCooldown;
    private final float missingHealthBonus;
    private final EnumSet<PlayerId> bonusAppliedTargets = EnumSet.noneOf(PlayerId.class);

    public SpinningOne(PlayerId owner, Vector3 center, float length, float width, float damage,
                       float healOnHit, float missingHealthBonus, float life,
                       String texturePath, int effectIndex) {
        this.owner = owner; this.position.set(center); this.length = length; this.width = width;
        this.baseDamage = damage; this.healOnHit = healOnHit; this.remainingLife = life;
        this.missingHealthBonus = Math.max(0, missingHealthBonus);
        this.texturePath = texturePath; this.effectIndex = effectIndex;
    }
    @Override public void update(float delta) {
        angle = (angle + delta * 250f) % 360f;
        hitCooldown -= delta;
        remainingLife -= delta;
    }
    public Vector3 direction(Vector3 out) {
        float radians = angle * MathUtils.degreesToRadians;
        return out.set(MathUtils.cos(radians), 0, MathUtils.sin(radians));
    }
    public float angle() { return angle; }
    public boolean damageReady() { return hitCooldown <= 0; }
    public void resetDamagePulse() { hitCooldown = .38f; }
    /** 同じ敵へ多段ヒットしても、発動時の減少HP加算を1回だけ返す。 */
    public float consumeMissingHealthBonus(PlayerId target) {
        return bonusAppliedTargets.add(target) ? missingHealthBonus : 0;
    }
    @Override public boolean expired() { return remainingLife <= 0; }
}
