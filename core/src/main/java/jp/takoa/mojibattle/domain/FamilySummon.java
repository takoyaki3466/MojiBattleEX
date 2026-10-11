package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class FamilySummon implements CombatObject {
    public final PlayerId owner;
    public final String texturePath;
    public final float maximumHp;
    public final float attackDamage;
    public final float projectileSpeed;
    public final int effectIndex;
    public final float orbitRadius;
    private final Vector3 position = new Vector3();
    private final float attackInterval;
    private float hp;
    private float attackCooldown;
    private float orbitAngle;

    public FamilySummon(PlayerId owner, Vector3 spawn, String texturePath, float maximumHp, float attackDamage,
                        float attackInterval, float projectileSpeed, int effectIndex, float orbitAngle, float orbitRadius) {
        this.owner = owner;
        this.position.set(spawn);
        this.texturePath = texturePath;
        this.maximumHp = maximumHp;
        this.hp = maximumHp;
        this.attackDamage = attackDamage;
        this.attackInterval = attackInterval;
        this.attackCooldown = attackInterval * .35f;
        this.projectileSpeed = projectileSpeed;
        this.effectIndex = effectIndex;
        this.orbitAngle = orbitAngle;
        this.orbitRadius = orbitRadius;
    }

    @Override public void update(float delta) {
        attackCooldown = Math.max(0, attackCooldown - delta);
        orbitAngle += delta * .55f;
    }
    public boolean attackReady() { return attackCooldown <= 0; }
    public void resetAttack() { attackCooldown = attackInterval; }
    public float receiveDamage(float damage) { float before = hp; hp = Math.max(0, hp - Math.max(0, damage)); return before - hp; }
    public float hpRatio() { return maximumHp <= 0 ? 0 : hp / maximumHp; }
    public float orbitAngle() { return orbitAngle; }
    public float collisionRadius() { return .72f; }
    @Override public boolean expired() { return hp <= 0; }
    @Override public Vector3 position() { return position; }
}
