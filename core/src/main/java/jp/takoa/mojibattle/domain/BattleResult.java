package jp.takoa.mojibattle.domain;

/**
 * 試合終了時の勝者と終了理由を表す不変の結果値。
 *
 * @param winner 勝者。引き分けの場合は {@code null}
 * @param reason 試合が終了した理由
 */
public record BattleResult(PlayerId winner, Reason reason) {
    /** 試合の終了理由。 */
    public enum Reason {
        KNOCKOUT,
        TIMEOUT,
        DRAW
    }

    /**
     * 勝者が存在しない結果かを返す。
     *
     * @return 引き分けの場合は {@code true}
     */
    public boolean draw() {
        return winner == null;
    }
}
