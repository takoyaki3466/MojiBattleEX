package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/** 巨大な「1」を回転させ、各周回で全長に触れたすべての敵を攻撃する判定オブジェクト。 */
public final class SpinningOne extends PositionedCombatObject {
    private static final float DAMAGE_PULSE_SECONDS = .38f;
    public final PlayerId owner;
    public final float length, width, baseDamage, healOnHit;
    public final String texturePath;
    public final int effectIndex;
    private float angle;
    private float remainingLife;
    private float hitCooldown;
    private final float missingHealthBonusPerPulse;

    public SpinningOne(PlayerId owner, Vector3 center, float length, float width, float damage,
                       float healOnHit, float missingHealthBonus, float life,
                       String texturePath, int effectIndex) {
        this.owner = owner; this.position.set(center); this.length = length; this.width = width;
        this.baseDamage = damage; this.healOnHit = healOnHit; this.remainingLife = life;
        int maximumDamagePulses = Math.max(1, MathUtils.ceil(life / DAMAGE_PULSE_SECONDS));
        this.missingHealthBonusPerPulse = Math.max(0, missingHealthBonus) / maximumDamagePulses;
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
    public void resetDamagePulse() { hitCooldown = DAMAGE_PULSE_SECONDS; }
    /** 減少HPぶんの追加ダメージを、必殺技の最大ヒット回数へ均等分割して返す。 */
    public float missingHealthBonusPerPulse() { return missingHealthBonusPerPulse; }
    @Override public boolean expired() { return remainingLife <= 0; }
}
