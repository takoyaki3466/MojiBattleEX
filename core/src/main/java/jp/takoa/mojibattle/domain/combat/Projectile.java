package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 飛翔、曲射、往復、追撃、クリティカルなどの攻撃特性を保持する投射物。
 */
public final class Projectile extends PositionedCombatObject {
    private static final float REPEATED_HIT_COOLDOWN = .32f;
    private static final float RETURN_COMPLETION_DISTANCE_SQUARED = .5f;

    public final long attackId;
    public final PlayerId owner;
    public final String glyph;
    public final String texturePath;
    public final String effectId;
    public final Vector3 velocity = new Vector3();
    public final float radius;
    public final float baseDamage;
    public final float stunSeconds;
    public final float bindSeconds;
    public final boolean boomerang;
    public final float boomerangDistance;
    public final float ultimatePointsOnHit;

    private final Vector3 origin = new Vector3();
    private float remainingLife;
    private boolean consumed;
    private boolean returning;
    private float hitCooldown;
    private float curveStrength;
    private boolean hasCriticalProfile;
    private boolean criticalEnabled;
    private float criticalRate;
    private float criticalDamageMultiplier;
    private String followUpGlyph;
    private String followUpTexture;
    private float followUpDamage;
    private float followUpRadius;
    private float followUpSpeed;
    private boolean ignoreDefense;
    private boolean ignoreDefenseOnCritical;
    private boolean ultimateAttack;
    private CharacterRuntime source;

    public Projectile(long attackId, PlayerId owner, String glyph, String texturePath, String effectId, Vector3 start, Vector3 velocity, float radius, float baseDamage, float stunSeconds, float life) {
        this(attackId, owner, glyph, texturePath, effectId, start, velocity, radius, baseDamage, stunSeconds, 0, life, false, 0, 0);
    }

    public Projectile(long attackId, PlayerId owner, String glyph, String texturePath, String effectId, Vector3 start, Vector3 velocity, float radius, float baseDamage, float stunSeconds, float bindSeconds, float life, boolean boomerang, float boomerangDistance, float pointsOnHit) {
        this.attackId = attackId;
        this.owner = owner;
        this.glyph = glyph;
        this.texturePath = texturePath;
        this.effectId = effectId;
        position.set(start);
        origin.set(start);
        this.velocity.set(velocity);
        this.radius = radius;
        this.baseDamage = baseDamage;
        this.stunSeconds = stunSeconds;
        this.bindSeconds = bindSeconds;
        remainingLife = life;
        this.boomerang = boomerang;
        this.boomerangDistance = boomerangDistance;
        ultimatePointsOnHit = pointsOnHit;
    }

    @Override
    public void update(float delta) {
        if (curveStrength != 0) {
            velocity.rotate(Vector3.Y, curveStrength * delta);
        }

        position.mulAdd(velocity, delta);
        hitCooldown = Math.max(0, hitCooldown - delta);

        float boomerangDistanceSquared = boomerangDistance * boomerangDistance;

        if (boomerang && !returning && position.dst2(origin) >= boomerangDistanceSquared) {
            returning = true;
            velocity.scl(-1);
        }

        if (returning && position.dst2(origin) < RETURN_COMPLETION_DISTANCE_SQUARED) {
            consumed = true;
        }

        remainingLife -= delta;
    }

    @Override
    public boolean expired() {
        return consumed || remainingLife <= 0;
    }

    public void consume() {
        consumed = true;
    }

    public boolean persistentOnHit() {
        return boomerang;
    }

    public boolean hitReady() {
        return hitCooldown <= 0;
    }

    public void markHit() {
        hitCooldown = REPEATED_HIT_COOLDOWN;
    }

    /**
     * 毎秒の旋回角度を追加し、この投射物を返す。
     */
    public Projectile curve(float degreesPerSecond) {
        curveStrength += degreesPerSecond;
        return this;
    }

    /**
     * 投射物固有のクリティカル設定を付与し、この投射物を返す。
     */
    public Projectile criticalProfile(boolean enabled, float rate, float damageMultiplier) {
        hasCriticalProfile = true;
        criticalEnabled = enabled;
        criticalRate = rate;
        criticalDamageMultiplier = damageMultiplier;

        return this;
    }

    public boolean hasCriticalProfile() {
        return hasCriticalProfile;
    }

    public boolean criticalEnabled() {
        return criticalEnabled;
    }

    public float criticalRate() {
        return criticalRate;
    }

    public float criticalDamageMultiplier() {
        return criticalDamageMultiplier;
    }

    /**
     * 命中後に生成する追撃弾の表示と攻撃値を設定し、この投射物を返す。
     */
    public Projectile followUp(String glyph, String texture, float damage, float radius, float speed) {
        followUpGlyph = glyph;
        followUpTexture = texture;
        followUpDamage = damage;
        followUpRadius = radius;
        followUpSpeed = speed;
        return this;
    }

    public boolean hasFollowUp() {
        return followUpTexture != null;
    }

    public String followUpGlyph() {
        return followUpGlyph;
    }

    public String followUpTexture() {
        return followUpTexture;
    }

    public float followUpDamage() {
        return followUpDamage;
    }

    public float followUpRadius() {
        return followUpRadius;
    }

    public float followUpSpeed() {
        return followUpSpeed;
    }

    /**
     * 必殺技由来か、防御倍率を無視するかを設定し、この投射物を返す。
     */
    public Projectile attackTraits(boolean ultimateAttack, boolean ignoreDefense) {
        this.ultimateAttack = ultimateAttack;
        this.ignoreDefense = ignoreDefense;
        return this;
    }

    public Projectile attackTraits(boolean ultimateAttack, boolean ignoreDefense,
                                   boolean ignoreDefenseOnCritical) {
        this.ultimateAttack = ultimateAttack;
        this.ignoreDefense = ignoreDefense;
        this.ignoreDefenseOnCritical = ignoreDefenseOnCritical;
        return this;
    }

    /**
     * 同じプレイヤーに複数のメインがいる場合に、攻撃倍率とゲージ加算の所有者を指定する。
     */
    public Projectile source(CharacterRuntime source) {
        this.source = source;
        return this;
    }

    public CharacterRuntime source() {
        return source;
    }

    public boolean ignoresDefense() {
        return ignoreDefense;
    }

    public boolean ignoresDefenseOnCritical() {
        return ignoreDefenseOnCritical;
    }

    public boolean ultimateAttack() {
        return ultimateAttack;
    }
}
