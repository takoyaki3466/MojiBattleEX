package jp.takoa.mojibattle.domain;

public final class BattleTimer {
    private final float duration;
    private float elapsed;

    public BattleTimer(float duration) {
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive");
        this.duration = duration;
    }
    public void update(float delta) { elapsed = Math.min(duration, elapsed + Math.max(0, delta)); }
    public float elapsed() { return elapsed; }
    public float remaining() { return Math.max(0, duration - elapsed); }
    public boolean expired() { return elapsed >= duration; }
    public void setElapsed(float value) { elapsed = Math.min(duration, Math.max(0, value)); }
}
