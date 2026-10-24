package jp.takoa.mojibattle.battle.status;

/**
 * 特殊浮動小数点値を使わず、有限時間または永続を表す。
 *
 * <h2>関係</h2>
 * <p>{@link StatusSpec}が必ず一つ保持し、ホストの状態更新処理から参照される。
 * 永続性は{@code Float.POSITIVE_INFINITY}ではなく明示フラグで表す。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * StatusDuration shortBuff = StatusDuration.seconds(5f);
 * StatusDuration aura = StatusDuration.permanent();
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>非有限値の直列化・比較・減算に起因する不具合を避け、有限と永続を型の不変条件で区別するために作成された。</p>
 *
 * @param seconds       有限期間の正の秒数。永続時は0
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
