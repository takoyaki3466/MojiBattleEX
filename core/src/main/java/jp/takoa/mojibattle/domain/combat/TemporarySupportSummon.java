package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.status.StatusEffectSystem;

/** 補助分裂中だけ存在する、個体IDと独立HPを持つ召喚戦闘個体。 */
public final class TemporarySupportSummon extends PositionedCombatObject {
    public final PlayerId owner;
    public final String sourceSupportInstanceId;
    public final String instanceId;
    public final String definitionId;
    public final String glyph;
    public final String texturePath;
    public final float attackDamage;
    public final float attackInterval;
    public final float moveSpeed;
    public final String commonTargetInstanceId;
    public final boolean recombineOnEnd;
    public final StatusEffectSystem statuses;
    private final float maximumHp;
    private float hp;
    private float attackCooldown;
    private float remaining;

    public TemporarySupportSummon(PlayerId owner, String sourceSupportInstanceId, String instanceId,
            String definitionId, String glyph, String texturePath, Vector3 start, float maximumHp,
            float attackDamage, float attackInterval, float moveSpeed, String commonTargetInstanceId,
            float durationSeconds, boolean recombineOnEnd) {
        this.owner = owner;
        this.sourceSupportInstanceId = sourceSupportInstanceId;
        this.instanceId = instanceId;
        this.definitionId = definitionId;
        this.glyph = glyph;
        this.texturePath = texturePath;
        position.set(start);
        this.maximumHp = Math.max(1, maximumHp);
        hp = this.maximumHp;
        this.attackDamage = attackDamage;
        this.attackInterval = attackInterval;
        this.moveSpeed = moveSpeed;
        this.commonTargetInstanceId = commonTargetInstanceId;
        remaining = durationSeconds;
        this.recombineOnEnd = recombineOnEnd;
        statuses = new StatusEffectSystem(owner);
    }

    @Override public void update(float delta) {
        remaining -= Math.max(0, delta);
        attackCooldown = Math.max(0, attackCooldown - delta);
        statuses.update(delta);
    }
    @Override public boolean expired() { return hp <= 0 || remaining <= 0; }
    public boolean attackReady() { return !expired() && attackCooldown <= 0; }
    public void resetAttack() { attackCooldown = attackInterval; }
    public float hp() { return hp; }
    public float maximumHp() { return maximumHp; }
    public float receiveDamage(float amount) { float before=hp; hp=Math.max(0,hp-Math.max(0,amount)); return before-hp; }
    public void heal(float amount) { if (!expired() && Float.isFinite(amount) && amount>0) hp=Math.min(maximumHp,hp+amount); }
    public float remaining() { return remaining; }
}
