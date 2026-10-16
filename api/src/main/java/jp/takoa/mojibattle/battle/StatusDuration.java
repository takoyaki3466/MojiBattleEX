package jp.takoa.mojibattle.battle;

/**
 * 特殊浮動小数点値を使わず、有限時間または永続を表す。
 * @param seconds 有限期間の正の秒数。永続時は0
 * @param permanentFlag 永続状態ならtrue
 */
public record StatusDuration(float seconds, boolean permanentFlag) {
    public StatusDuration {
        if (permanentFlag) {
            seconds = 0;
        } else if (!Float.isFinite(seconds) || seconds <= 0) {
            throw new IllegalArgumentException("Finite status duration must be positive");
        }
    }

    public static StatusDuration seconds(float seconds) {
        return new StatusDuration(seconds, false);
    }

    public static StatusDuration permanent() {
        return new StatusDuration(0, true);
    }
}
