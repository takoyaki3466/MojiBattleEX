package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 所有者の周囲を旋回し、独立したHPと攻撃間隔を持つ召喚ユニット。
 */
public final class FamilySummon extends PositionedCombatObject {
    public final PlayerId owner;
    public final String texturePath;
    public final float maximumHp;
    public final float defense;
    public final float attackDamage;
    public final float projectileSpeed;
    public final String effectId;
    public final float orbitRadius;
    private final float attackInterval;
    private float hp;
    private float attackCooldown;
    private float orbitAngle;

    public FamilySummon(PlayerId owner, Vector3 spawn, String texturePath, float maximumHp, float defense, float attackDamage, float attackInterval, float projectileSpeed, String effectId, float orbitAngle, float orbitRadius) {
        this.owner = owner;
        this.position.set(spawn);
        this.texturePath = texturePath;
        this.maximumHp = maximumHp;
        this.hp = maximumHp;
        this.defense = Math.max(0, defense);
        this.attackDamage = attackDamage;
        this.attackInterval = attackInterval;
        this.attackCooldown = attackInterval * .35f;
        this.projectileSpeed = projectileSpeed;
        this.effectId = effectId;
        this.orbitAngle = orbitAngle;
        this.orbitRadius = orbitRadius;
    }

    @Override
    public void update(float delta) {
        attackCooldown = Math.max(0, attackCooldown - delta);
        orbitAngle += delta * .55f;
    }

    public boolean attackReady() {
        return attackCooldown <= 0;
    }

    public void resetAttack() {
        attackCooldown = attackInterval;
    }

    public float receiveDamage(float damage) {
        float before = hp;
        float reducedDamage = Math.max(0, damage) * 100f / (100f + defense);
        hp = Math.max(0, hp - reducedDamage);
        return before - hp;
    }

    public float hpRatio() {
        return maximumHp <= 0 ? 0 : hp / maximumHp;
    }

    public float orbitAngle() {
        return orbitAngle;
    }

    public float collisionRadius() {
        return .72f;
    }

    @Override
    public boolean expired() {
        return hp <= 0;
    }
}
