package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 警告時間の後に直線範囲へ一度だけダメージを発生させる戦場オブジェクト。
 */
public final class LineStrike extends PositionedCombatObject {
    public final PlayerId owner;
    public final Vector3 direction = new Vector3();
    public final float length, width, baseDamage;
    public final String warningEffectId, impactEffectId;
    public final String objectTexture;
    private final float warningDuration, effectDuration;
    private float elapsed;
    private boolean damagePending;
    private boolean mainUltimate;

    public LineStrike(PlayerId owner, Vector3 start, Vector3 direction, float length, float width, float damage, float warning, float effectDuration, String warningEffectId, String impactEffectId, String objectTexture) {
        this.owner = owner;
        position.set(start);
        this.direction.set(direction).set(direction.x, 0, direction.z).nor();
        this.length = length;
        this.width = width;
        baseDamage = damage;
        warningDuration = warning;
        this.effectDuration = effectDuration;
        this.warningEffectId = warningEffectId;
        this.impactEffectId = impactEffectId;
        this.objectTexture = objectTexture;
    }

    @Override
    public void update(float delta) {
        float before = elapsed;
        elapsed += delta;
        if (before < warningDuration && elapsed >= warningDuration) {
            damagePending = true;
        }
    }

    /**
     * 着弾フレームに一度だけ立つダメージ要求を取得し、その場で消費する。
     *
     * @return 未処理の着弾があれば {@code true}
     */
    public boolean takeDamagePending() {
        boolean result = damagePending;
        damagePending = false;
        return result;
    }

    public boolean impacting() {
        return elapsed >= warningDuration;
    }

    /**
     * 必殺技由来として必殺技用倍率を適用するよう印を付ける。
     */
    public LineStrike mainUltimate() {
        mainUltimate = true;
        return this;
    }

    public boolean isMainUltimate() {
        return mainUltimate;
    }

    public float warningProgress() {
        return warningDuration <= 0 ? 1 : Math.min(1, elapsed / warningDuration);
    }

    @Override
    public boolean expired() {
        return elapsed >= warningDuration + effectDuration;
    }
}
