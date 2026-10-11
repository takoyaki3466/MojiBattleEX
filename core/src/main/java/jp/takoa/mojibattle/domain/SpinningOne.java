package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** Giant rotating '1' hitbox. Each sweep can hit every enemy touching its full length. */
public final class SpinningOne implements CombatObject {
    public final PlayerId owner;
    public final float length, width, baseDamage, healOnHit;
    public final String texturePath;
    public final int effectIndex;
    private final Vector3 position = new Vector3();
    private float angle;
    private float remainingLife;
    private float hitCooldown;

    public SpinningOne(PlayerId owner, Vector3 center, float length, float width, float damage,
                       float healOnHit, float life, String texturePath, int effectIndex) {
        this.owner = owner; this.position.set(center); this.length = length; this.width = width;
        this.baseDamage = damage; this.healOnHit = healOnHit; this.remainingLife = life;
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
    @Override public boolean expired() { return remainingLife <= 0; }
    @Override public Vector3 position() { return position; }
}
