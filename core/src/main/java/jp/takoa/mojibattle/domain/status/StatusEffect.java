package jp.takoa.mojibattle.domain.status;

import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 付与元、効果量、残り時間および重複規則を保持する可変の状態効果。
 */
public final class StatusEffect {
    public final String id;
    public final PlayerId source;
    public PlayerId target;
    public final StatusType type;
    public final float magnitude;
    public final StatusStackRule stackRule;
    public final int maximumStacks;
    public int stacks = 1;
    public float remaining;
    public float tickRemaining;
    public float tickInterval = 1f;
    public float ultimatePointsPerTick;

    public StatusEffect(String id, PlayerId source, StatusType type, float magnitude, float duration) {
        this(id, source, type, magnitude, duration, StatusStackRule.REFRESH, 1);
    }

    public StatusEffect(String id, PlayerId source, StatusType type, float magnitude, float duration, StatusStackRule stackRule, int maximumStacks) {
        this.id = id;
        this.source = source;
        this.type = type;
        this.magnitude = magnitude;
        this.stackRule = stackRule;
        this.maximumStacks = Math.max(1, maximumStacks);
        remaining = duration;
        tickRemaining = 1f;
    }

    /**
     * 定期処理間隔を正数へ補正して設定する。
     */
    public StatusEffect tickEvery(float seconds) {
        tickInterval = Math.max(.01f, seconds);
        tickRemaining = tickInterval;
        return this;
    }

    /**
     * 1回の定期処理で付与する必殺ポイントを非負値へ補正して設定する。
     */
    public StatusEffect ultimatePointsPerTick(float points) {
        ultimatePointsPerTick = Math.max(0, points);
        return this;
    }
}
