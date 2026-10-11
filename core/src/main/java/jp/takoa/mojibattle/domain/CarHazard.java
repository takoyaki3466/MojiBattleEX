package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

/**
 * A long-lived, bouncing car which periodically damages and stuns everything it overlaps.
 */
public final class CarHazard implements CombatObject {
    public final PlayerId owner;
    public final Vector3 velocity = new Vector3();
    public final float radius, baseDamage, stunSeconds;
    public final String texturePath;
    public final int effectIndex;
    public final boolean friendlyFire;
    private final Vector3 position = new Vector3();
    private final float arenaHalfExtent;
    private float remainingLife;
    private float hitCooldown;

    public CarHazard(PlayerId owner, Vector3 start, Vector3 direction, float speed, float radius, float damage, float stunSeconds, float life, float arenaHalfExtent, String texturePath, int effectIndex) {
        this(owner, start, direction, speed, radius, damage, stunSeconds, life, arenaHalfExtent, texturePath, effectIndex, false);
    }

    public CarHazard(PlayerId owner, Vector3 start, Vector3 direction, float speed, float radius, float damage, float stunSeconds, float life, float arenaHalfExtent, String texturePath, int effectIndex, boolean friendlyFire) {
        this.owner = owner;
        this.position.set(start).add(0, .2f, 0);
        this.velocity.set(direction.x, 0, direction.z).nor().scl(speed);
        this.radius = radius;
        this.baseDamage = damage;
        this.stunSeconds = stunSeconds;
        this.remainingLife = life;
        this.arenaHalfExtent = arenaHalfExtent;
        this.texturePath = texturePath;
        this.effectIndex = effectIndex;
        this.friendlyFire = friendlyFire;
    }

    @Override
    public void update(float delta) {
        position.mulAdd(velocity, delta);
        float edge = Math.max(1, arenaHalfExtent - radius * .45f);
        if (position.x < -edge || position.x > edge) {
            position.x = Math.max(-edge, Math.min(edge, position.x));
            velocity.x = -velocity.x;
        }
        if (position.z < -edge || position.z > edge) {
            position.z = Math.max(-edge, Math.min(edge, position.z));
            velocity.z = -velocity.z;
        }
        hitCooldown = Math.max(0, hitCooldown - delta);
        remainingLife -= delta;
    }

    public boolean damageReady() {
        return hitCooldown <= 0;
    }

    public void resetDamagePulse() {
        hitCooldown = .48f;
    }

    @Override
    public boolean expired() {
        return remainingLife <= 0;
    }

    @Override
    public Vector3 position() {
        return position;
    }
}
