package jp.takoa.mojibattle.loading;

/**
 * ロードタスクの実行状態。
 */
public enum LoadTaskState {
    /** まだ開始していない。 */
    PENDING("WAIT"),
    /** 実行中。 */
    RUNNING("RUNNING"),
    /** 正常に完了した。 */
    DONE("DONE"),
    /** 失敗した。成功扱いにはしない。 */
    FAILED("FAILED");

    private final String debugLabel;

    LoadTaskState(String debugLabel) {
        this.debugLabel = debugLabel;
    }

    /**
     * デバッグ表示とクラッシュレポートで使用する短い英字ラベル。
     */
    public String debugLabel() {
        return debugLabel;
    }
}
