package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;

/**
 * 戦闘イベントから生成され、描画だけに使用される一時的な演出状態。
 */
public final class VisualEffect extends PositionedCombatObject {
    public final String effectId;
    public final float maximumScale;
    public final String texturePath;
    private final float duration;
    private float elapsed;

    public VisualEffect(Vector3 position, String effectId, float maximumScale, float duration) {
        this(position, effectId, maximumScale, duration, null);
    }

    public VisualEffect(Vector3 position, String effectId, float maximumScale, float duration, String texturePath) {
        this.position.set(position);
        this.effectId = effectId;
        this.maximumScale = maximumScale;
        this.duration = duration;
        this.texturePath = texturePath;
    }

    @Override
    public void update(float delta) {
        elapsed += Math.max(0, delta);
    }

    @Override
    public boolean expired() {
        return elapsed >= duration;
    }

    public float progress() {
        return duration <= 0 ? 1 : Math.min(1, elapsed / duration);
    }

    public float scale() {
        return maximumScale * (0.35f + progress() * 0.65f);
    }
}
