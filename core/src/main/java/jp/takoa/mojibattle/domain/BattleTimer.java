package jp.takoa.mojibattle.domain;

/**
 * 指定された試合時間の経過と残り時間を範囲内に保って管理する。
 */
public final class BattleTimer {
    private final float duration;
    private float elapsed;

    /**
     * @param duration 試合時間（秒）
     * @throws IllegalArgumentException 試合時間が0以下の場合
     */
    public BattleTimer(float duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        this.duration = duration;
    }

    /**
     * 負数を無視し、終了時刻を超えない範囲で時間を進める。
     */
    public void update(float delta) {
        elapsed = Math.min(duration, elapsed + Math.max(0, delta));
    }

    public float elapsed() {
        return elapsed;
    }

    public float remaining() {
        return Math.max(0, duration - elapsed);
    }

    public boolean expired() {
        return elapsed >= duration;
    }

    /**
     * 経過時間を0から試合時間までの範囲へ補正して設定する。
     */
    public void setElapsed(float value) {
        elapsed = Math.min(duration, Math.max(0, value));
    }
}
