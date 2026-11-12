package jp.takoa.mojibattle.loading;

/**
 * ロード処理を構成する一つの作業単位の状態。
 *
 * <p>状態の更新は {@link LoadingTracker} だけが行う。UIはこのクラスを読み取り専用で参照する。</p>
 */
public final class LoadTask {
    private final String debugName;
    private final String messageKey;
    private final float weight;

    private LoadTaskState state = LoadTaskState.PENDING;
    private long startNanos;
    private long endNanos;
    private float fraction;
    private int completedUnits;
    private int totalUnits;
    private String detail = "";
    private String errorSummary;

    LoadTask(String debugName, String messageKey, float weight) {
        if (debugName == null || debugName.isBlank()) {
            throw new IllegalArgumentException("Load task name is required");
        }
        if (!(weight > 0) || Float.isInfinite(weight)) {
            throw new IllegalArgumentException("Load task weight must be positive: " + weight);
        }
        this.debugName = debugName;
        this.messageKey = messageKey;
        this.weight = weight;
    }

    /** デバッグ表示用の英字タスク名。 */
    public String debugName() {
        return debugName;
    }

    /** 通常表示で使用する翻訳キー。 */
    public String messageKey() {
        return messageKey;
    }

    public float weight() {
        return weight;
    }

    public LoadTaskState state() {
        return state;
    }

    /** タスク内部の進捗（0〜1）。完了時は1。 */
    public float fraction() {
        return state == LoadTaskState.DONE ? 1f : fraction;
    }

    /** 件数で進捗を管理するタスクの完了件数。件数管理しない場合は0。 */
    public int completedUnits() {
        return completedUnits;
    }

    /** 件数で進捗を管理するタスクの総件数。件数管理しない場合は0。 */
    public int totalUnits() {
        return totalUnits;
    }

    /** 現在処理中の対象（リソース名など）。完了後は最後に設定された補足情報を保持する。 */
    public String detail() {
        return detail;
    }

    /** 失敗時のエラー概要。失敗していなければnull。 */
    public String errorSummary() {
        return errorSummary;
    }

    long startNanos() {
        return startNanos;
    }

    long endNanos() {
        return endNanos;
    }

    void start(long now) {
        if (state != LoadTaskState.PENDING) {
            throw new IllegalStateException("Load task already started: " + debugName);
        }
        state = LoadTaskState.RUNNING;
        startNanos = now;
    }

    void units(int completed, int total, String currentDetail) {
        requireRunning();
        if (total < 0 || completed < 0 || completed > total) {
            throw new IllegalArgumentException("Invalid load task units " + completed + "/" + total);
        }
        completedUnits = Math.max(completedUnits, completed);
        totalUnits = total;
        float next = total == 0 ? 0f : (float) completedUnits / total;
        // 完了報告前に1へ到達させない。100%は全タスクの成功時だけに表示する。
        fraction = Math.max(fraction, Math.min(next, .999f));
        detail = currentDetail == null ? "" : currentDetail;
    }

    void detail(String currentDetail) {
        requireRunning();
        detail = currentDetail == null ? "" : currentDetail;
    }

    void complete(long now) {
        requireRunning();
        state = LoadTaskState.DONE;
        endNanos = now;
        fraction = 1f;
        completedUnits = totalUnits;
    }

    void fail(long now, String summary) {
        if (state == LoadTaskState.DONE || state == LoadTaskState.FAILED) {
            throw new IllegalStateException("Load task already finished: " + debugName);
        }
        if (state == LoadTaskState.PENDING) {
            startNanos = now;
        }
        state = LoadTaskState.FAILED;
        endNanos = now;
        errorSummary = summary == null || summary.isBlank() ? "unknown error" : summary;
    }

    private void requireRunning() {
        if (state != LoadTaskState.RUNNING) {
            throw new IllegalStateException("Load task is not running: " + debugName + " (" + state + ")");
        }
    }
}
