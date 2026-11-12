package jp.takoa.mojibattle.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * 段階的なロード処理の状態と進捗率を管理する。描画コードからは独立しており、
 * 通常表示とデバッグ表示の両方が同じ状態を参照する。
 *
 * <p>進捗率は完了したタスクの重みと、件数管理するタスクの完了件数から算出する。
 * 時間経過では増加せず、原則として後退しない。全タスクが成功するまで100%にはならない。
 * GLスレッドからのみ更新すること。</p>
 */
public final class LoadingTracker {
    private final List<LoadTask> tasks = new ArrayList<>();
    private final LongSupplier clock;
    private long startedNanos = -1;
    private long finishedNanos = -1;
    private float reportedProgress;
    private LoadTask failedTask;

    public LoadingTracker() {
        this(System::nanoTime);
    }

    public LoadingTracker(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * タスクを末尾へ登録する。開始後は追加できない。
     */
    public LoadTask add(String debugName, String messageKey, float weight) {
        if (startedNanos >= 0) {
            throw new IllegalStateException("Cannot add load tasks after loading started");
        }
        LoadTask task = new LoadTask(debugName, messageKey, weight);
        tasks.add(task);
        return task;
    }

    public void start(LoadTask task) {
        requireOwned(task);
        requireNotFailed();
        long now = clock.getAsLong();
        if (startedNanos < 0) {
            startedNanos = now;
        }
        task.start(now);
    }

    public void units(LoadTask task, int completed, int total, String detail) {
        requireOwned(task);
        task.units(completed, total, detail);
    }

    public void detail(LoadTask task, String detail) {
        requireOwned(task);
        task.detail(detail);
    }

    public void complete(LoadTask task) {
        requireOwned(task);
        long now = clock.getAsLong();
        task.complete(now);
        if (finished()) {
            finishedNanos = now;
        }
    }

    /**
     * タスクを失敗として記録する。失敗したタスクの進捗は加算しない。
     */
    public void fail(LoadTask task, Throwable error) {
        requireOwned(task);
        long now = clock.getAsLong();
        if (startedNanos < 0) {
            startedNanos = now;
        }
        task.fail(now, summarize(error));
        failedTask = task;
        finishedNanos = now;
    }

    /**
     * 0〜1の進捗率。前回より小さい値は返さない。全タスク成功時だけ1を返す。
     */
    public float progress() {
        if (tasks.isEmpty()) {
            return 0f;
        }
        if (finished()) {
            reportedProgress = 1f;
            return 1f;
        }
        float total = 0f;
        float done = 0f;
        for (LoadTask task : tasks) {
            total += task.weight();
            if (task.state() == LoadTaskState.DONE) {
                done += task.weight();
            } else if (task.state() == LoadTaskState.RUNNING) {
                done += task.weight() * task.fraction();
            }
        }
        float raw = Math.min(.999f, done / total);
        reportedProgress = Math.max(reportedProgress, raw);
        return reportedProgress;
    }

    /**
     * 表示用の整数パーセント（0〜100）。小数は切り捨て、完了前に100を表示しない。
     */
    public int percent() {
        float value = progress();
        if (value >= 1f) {
            return 100;
        }
        return Math.min(99, (int) Math.floor(value * 100f));
    }

    /** 全タスクが成功したか。 */
    public boolean finished() {
        if (tasks.isEmpty() || failedTask != null) {
            return false;
        }
        for (LoadTask task : tasks) {
            if (task.state() != LoadTaskState.DONE) {
                return false;
            }
        }
        return true;
    }

    public boolean failed() {
        return failedTask != null;
    }

    public LoadTask failedTask() {
        return failedTask;
    }

    /**
     * 実行中のタスク。なければ次に実行する待機中タスク、全て終了済みならnull。
     */
    public LoadTask current() {
        if (failedTask != null) {
            return failedTask;
        }
        for (LoadTask task : tasks) {
            if (task.state() == LoadTaskState.RUNNING) {
                return task;
            }
        }
        for (LoadTask task : tasks) {
            if (task.state() == LoadTaskState.PENDING) {
                return task;
            }
        }
        return null;
    }

    public List<LoadTask> tasks() {
        return List.copyOf(tasks);
    }

    public int completedCount() {
        int count = 0;
        for (LoadTask task : tasks) {
            if (task.state() == LoadTaskState.DONE) {
                count++;
            }
        }
        return count;
    }

    public int taskCount() {
        return tasks.size();
    }

    /** ロード開始からの経過時間。終了後は終了時点で停止する。 */
    public long elapsedNanos() {
        if (startedNanos < 0) {
            return 0;
        }
        long end = finishedNanos >= 0 ? finishedNanos : clock.getAsLong();
        return Math.max(0, end - startedNanos);
    }

    /** タスクの所要時間。実行中なら現在までの経過時間、未開始なら0。 */
    public long durationNanos(LoadTask task) {
        requireOwned(task);
        return switch (task.state()) {
            case PENDING -> 0;
            case RUNNING -> Math.max(0, clock.getAsLong() - task.startNanos());
            case DONE, FAILED -> Math.max(0, task.endNanos() - task.startNanos());
        };
    }

    /**
     * クラッシュレポートとログ向けの、状態を一覧化した複数行テキスト。
     */
    public String describe() {
        StringBuilder text = new StringBuilder();
        text.append("Progress: ").append(percent()).append("% (").append(completedCount()).append(" / ").append(taskCount()).append(" tasks)").append(System.lineSeparator());
        text.append("Elapsed: ").append(formatSeconds(elapsedNanos())).append(System.lineSeparator());
        for (LoadTask task : tasks) {
            text.append("  [").append(task.state().debugLabel()).append("] ").append(task.debugName());
            if (task.totalUnits() > 0) {
                text.append(" ").append(task.completedUnits()).append("/").append(task.totalUnits());
            }
            if (task.state() != LoadTaskState.PENDING) {
                text.append(" (").append(formatMillis(durationNanos(task))).append(")");
            }
            if (!task.detail().isEmpty()) {
                text.append(" - ").append(task.detail());
            }
            if (task.errorSummary() != null) {
                text.append(" ERROR: ").append(task.errorSummary());
            }
            text.append(System.lineSeparator());
        }
        return text.toString();
    }

    public static String formatMillis(long nanos) {
        return (nanos / 1_000_000L) + " ms";
    }

    public static String formatSeconds(long nanos) {
        return String.format(Locale.ROOT, "%.2f s", nanos / 1_000_000_000d);
    }

    static String summarize(Throwable error) {
        if (error == null) {
            return "unknown error";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private void requireOwned(LoadTask task) {
        if (task == null || !tasks.contains(task)) {
            throw new IllegalArgumentException("Load task does not belong to this tracker");
        }
    }

    private void requireNotFailed() {
        if (failedTask != null) {
            throw new IllegalStateException("Loading already failed at " + failedTask.debugName());
        }
    }
}
