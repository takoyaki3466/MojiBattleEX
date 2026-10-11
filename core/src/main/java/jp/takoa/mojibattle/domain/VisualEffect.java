package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

/** Render-only transient state emitted by deterministic combat events. */
public final class VisualEffect implements CombatObject {
    public final int effectIndex;
    public final float maximumScale;
    public final String texturePath;
    private final Vector3 position = new Vector3();
    private final float duration;
    private float elapsed;

    public VisualEffect(Vector3 position, int effectIndex, float maximumScale, float duration) {
        this(position,effectIndex,maximumScale,duration,null);
    }
    public VisualEffect(Vector3 position, int effectIndex, float maximumScale, float duration, String texturePath) {
        this.position.set(position); this.effectIndex = effectIndex; this.maximumScale = maximumScale; this.duration = duration; this.texturePath=texturePath;
    }
    @Override public void update(float delta) { elapsed += Math.max(0, delta); }
    @Override public boolean expired() { return elapsed >= duration; }
    @Override public Vector3 position() { return position; }
    public float progress() { return duration <= 0 ? 1 : Math.min(1, elapsed / duration); }
    public float scale() { return maximumScale * (0.35f + progress() * 0.65f); }
}
