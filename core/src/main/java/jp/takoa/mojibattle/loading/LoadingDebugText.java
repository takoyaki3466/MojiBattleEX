package jp.takoa.mojibattle.loading;

/**
 * デバッグモードで表示するロード状況の詳細テキストを、{@link LoadingTracker} の状態から組み立てる。
 *
 * <p>戦闘前と起動時のロード画面で共通に使う。追加の計測は行わず、トラッカーが記録済みの値だけを文字列化する。</p>
 */
public final class LoadingDebugText {
    private LoadingDebugText() {
    }

    /**
     * 例: {@code [LOAD] 4 / 8 (52%)}、{@code [CURRENT] Texture Loading 12/40 0.84 s}、{@code [TIME] 1.24 s}、
     * タスクごとの {@code [DONE] Character Data (42 ms)} など。
     */
    public static String format(LoadingTracker tracker) {
        StringBuilder text = new StringBuilder();
        text.append("[LOAD] ").append(tracker.completedCount()).append(" / ").append(tracker.taskCount()).append("  (").append(tracker.percent()).append("%)\n");
        LoadTask current = tracker.current();
        if (current != null && current.state() == LoadTaskState.RUNNING) {
            text.append("[CURRENT] ").append(current.debugName());
            if (current.totalUnits() > 0) {
                text.append(" ").append(current.completedUnits()).append("/").append(current.totalUnits());
            }
            text.append("  ").append(LoadingTracker.formatSeconds(tracker.durationNanos(current)));
            if (!current.detail().isEmpty()) {
                text.append("\n          ").append(current.detail());
            }
            text.append('\n');
        }
        text.append("[TIME] ").append(LoadingTracker.formatSeconds(tracker.elapsedNanos())).append('\n');
        for (LoadTask task : tracker.tasks()) {
            text.append('[').append(task.state().debugLabel()).append("] ").append(task.debugName());
            if (task.state() == LoadTaskState.DONE || task.state() == LoadTaskState.FAILED) {
                text.append(" (").append(LoadingTracker.formatMillis(tracker.durationNanos(task))).append(')');
            } else if (task.state() == LoadTaskState.RUNNING) {
                text.append(" (").append(LoadingTracker.formatMillis(tracker.durationNanos(task))).append(" ...)");
            }
            if (task.state() == LoadTaskState.DONE && !task.detail().isEmpty()) {
                text.append(" - ").append(task.detail());
            }
            if (task.errorSummary() != null) {
                text.append("\n    ERROR: ").append(task.errorSummary());
            }
            text.append('\n');
        }
        return text.toString();
    }
}
