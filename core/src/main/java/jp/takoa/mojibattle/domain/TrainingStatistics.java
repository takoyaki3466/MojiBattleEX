package jp.takoa.mojibattle.domain;

/**
 * トレーニング中の累積ダメージ、ヒット数および経過時間を集計する。
 */
public final class TrainingStatistics {
    private float damage;
    private float lastDamage;
    private int hits;
    private float sampleSeconds;

    /**
     * 集計対象の経過時間を、負数を無視して進める。
     */
    public void update(float delta) {
        sampleSeconds += Math.max(0, delta);
    }

    /**
     * 正のダメージを1ヒットとして記録する。
     */
    public void record(float value) {
        if (value > 0) {
            damage += value;
            lastDamage = value;
            hits++;
        }
    }

    /**
     * すべての集計値を初期状態へ戻す。
     */
    public void reset() {
        damage = 0;
        lastDamage = 0;
        hits = 0;
        sampleSeconds = 0;
    }

    public float damage() {
        return damage;
    }

    public float lastDamage() {
        return lastDamage;
    }

    public int hits() {
        return hits;
    }

    /**
     * @return 集計開始からの平均秒間ダメージ。経過時間が0の場合は0
     */
    public float dps() {
        return sampleSeconds > 0 ? damage / sampleSeconds : 0;
    }

    public float sampleSeconds() {
        return sampleSeconds;
    }
}
