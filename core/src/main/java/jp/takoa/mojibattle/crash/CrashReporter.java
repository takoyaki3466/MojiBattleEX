package jp.takoa.mojibattle.crash;

import jp.takoa.mojibattle.debug.DebugSettings;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.mod.GameApiVersion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * ロード中・戦闘中・捕捉されない例外などを、共通形式のクラッシュレポートとして書き出す窓口。
 *
 * <p>どの呼出し元からも同じ処理でレポートを生成する。同じ例外（原因例外として包まれた場合を含む）は
 * 一度だけ記録し、レポート生成中の再帰や書込み失敗では元の例外処理を妨げない。
 * このクラスのメソッドは例外を送出しない。</p>
 */
public final class CrashReporter {
    private static final int MAX_REPORTS_PER_SESSION = 16;
    private static final int RECENT_LOG_LINES = 60;
    private static final long RECENT_LOG_MAX_BYTES = 512 * 1024;
    private static final Object LOCK = new Object();
    private static final ThreadLocal<Boolean> REPORTING = ThreadLocal.withInitial(() -> false);

    /** 報告済み例外。Throwableは同値性を上書きしないため、WeakHashMapでも同一性で判定される。 */
    private static final Set<Throwable> REPORTED = Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile String gameVersion;
    private static volatile GameState gameState = GameState.STARTUP;
    private static volatile LoadingInfo loadingInfo;
    private static volatile CrashReportLocation location;
    private static int reportCount;

    private CrashReporter() {
    }

    /**
     * 配布ビルドのバージョン（JARのImplementation-Version等）を設定する。
     */
    public static void setGameVersion(String version) {
        gameVersion = version;
    }

    public static void setGameState(GameState state) {
        gameState = state == null ? GameState.MENU : state;
    }

    public static GameState gameState() {
        return gameState;
    }

    /**
     * ロード中に限り、現在のタスク名とロード状態を提供する。ロード終了時はnullを設定する。
     */
    public static void setLoadingInfo(LoadingInfo info) {
        loadingInfo = info;
    }

    /**
     * 保存先の解決結果を返す。初回呼出し時に解決する。
     */
    public static CrashReportLocation location() {
        CrashReportLocation resolved = location;
        if (resolved == null) {
            resolved = CrashReportLocation.detect();
            location = resolved;
        }
        return resolved;
    }

    /**
     * 既存の既定ハンドラーを保持したまま、捕捉されない例外のレポート生成を追加する。
     * 二重に設定しない。
     */
    public static void installUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof ReportingHandler) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new ReportingHandler(previous));
    }

    /**
     * 例外を報告する。既に報告済みの例外であれば何もしない。
     *
     * @param operation 発生した画面や処理の名称
     * @param error     発生した例外
     * @return 書き込んだレポート。報告済み・抑止・書込み失敗の場合は空
     */
    public static Optional<Path> report(String operation, Throwable error) {
        return report(operation, null, error, Thread.currentThread());
    }

    /**
     * 例外を、利用者向けの概要付きで報告する。
     */
    public static Optional<Path> report(String operation, String summary, Throwable error) {
        return report(operation, summary, error, Thread.currentThread());
    }

    /**
     * この例外（またはその原因例外）が既に報告済みか。
     */
    public static boolean alreadyReported(Throwable error) {
        if (error == null) {
            return false;
        }
        synchronized (LOCK) {
            for (Throwable item : chain(error)) {
                if (REPORTED.contains(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    static Optional<Path> report(String operation, String summary, Throwable error, Thread errorThread) {
        if (REPORTING.get()) {
            // レポート生成中に別の例外が発生した場合は再帰させない。
            safeStderr("MojiBattleEX: nested failure while writing a crash report: " + error);
            return Optional.empty();
        }
        REPORTING.set(true);
        try {
            synchronized (LOCK) {
                if (error != null) {
                    List<Throwable> chain = chain(error);
                    for (Throwable item : chain) {
                        if (REPORTED.contains(item)) {
                            return Optional.empty();
                        }
                    }
                    REPORTED.addAll(chain);
                }
                if (reportCount >= MAX_REPORTS_PER_SESSION) {
                    safeLog("Crash report suppressed because the per-session limit was reached", error);
                    return Optional.empty();
                }
                reportCount++;
            }
            return write(operation, summary, error, errorThread);
        } catch (Throwable reportFailure) {
            // ここで例外を送出すると元の例外情報が失われるため、標準エラーへのみ記録する。
            safeStderr("MojiBattleEX: crash report generation failed: " + reportFailure);
            return Optional.empty();
        } finally {
            REPORTING.set(false);
        }
    }

    private static Optional<Path> write(String operation, String summary, Throwable error, Thread errorThread) {
        ZonedDateTime timestamp = ZonedDateTime.now();
        CrashReportLocation target = location();
        // 起動時ロード（STARTUP）と戦闘前ロード（LOADING）の両方で、現在のタスクとロード状況を記録する。
        LoadingInfo loading = gameState == GameState.LOADING || gameState == GameState.STARTUP ? loadingInfo : null;
        String currentTask = loading == null ? null : safeGet(loading::currentTask);
        String loadingState = loading == null ? null : safeGet(loading::describe);
        String recentLog = recentLog();
        String reportThread = threadName(Thread.currentThread());

        CrashReportWriter.Result result = CrashReportWriter.write(target.candidates(), timestamp, (directory, fallback, failures) -> {
            String note = (fallback ? "FALLBACK " : "") + directory;
            if (target.primary() == null) {
                note += " (application image directory could not be determined)";
            }
            if (!failures.isEmpty()) {
                note += System.lineSeparator() + "  Failed locations: " + String.join("; ", failures);
            }
            return CrashReportFormatter.format(new CrashReportFormatter.CrashReportData(timestamp, versionOrUnknown(), GameApiVersion.GAME_VERSION, String.valueOf(GameApiVersion.API_VERSION), property("java.version") + " (" + property("java.vendor") + ")", property("java.runtime.name") + " " + property("java.runtime.version") + " / " + property("java.vm.name"), property("os.name") + " " + property("os.version") + " (" + property("os.arch") + ")", memory(), gameState, DiagnosticLog.currentScreen(), operation, currentTask, DebugSettings.enabled(), reportThread, threadName(errorThread), DiagnosticLog.loadedMods(), DiagnosticLog.selectedMainIds(), DiagnosticLog.selectedSupportIds(), summary, error, loadingState, note, String.valueOf(DiagnosticLog.file()), recentLog));
        });

        if (result.succeeded()) {
            if (result.usedFallback()) {
                safeLog("Crash report written to FALLBACK location " + result.file() + " because the primary location was unavailable: " + result.failures(), null);
            } else {
                safeLog("Crash report written to " + result.file(), null);
            }
            return Optional.of(result.file());
        }
        safeLog("Crash report could not be written to any location: " + result.failures(), error);
        return Optional.empty();
    }

    private static List<Throwable> chain(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        pending.add(error);
        while (!pending.isEmpty() && seen.size() < 64) {
            Throwable current = pending.removeFirst();
            if (current == null || !seen.add(current)) {
                continue;
            }
            if (current.getCause() != null) {
                pending.add(current.getCause());
            }
        }
        return List.copyOf(seen);
    }

    private static String recentLog() {
        try {
            Path file = DiagnosticLog.file();
            if (file == null || !Files.isRegularFile(file) || Files.size(file) > RECENT_LOG_MAX_BYTES) {
                return file == null ? null : "(diagnostic log unavailable or too large to embed; see " + file + ")";
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - RECENT_LOG_LINES);
            return String.join(System.lineSeparator(), lines.subList(from, lines.size()));
        } catch (Throwable error) {
            return "(diagnostic log could not be read: " + error.getClass().getSimpleName() + ")";
        }
    }

    private static String versionOrUnknown() {
        String version = gameVersion;
        return version == null || version.isBlank() ? "unknown (development build)" : version;
    }

    private static String memory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long mb = 1024 * 1024;
            return "used=" + (runtime.totalMemory() - runtime.freeMemory()) / mb + "MB, total=" + runtime.totalMemory() / mb + "MB, max=" + runtime.maxMemory() / mb + "MB, processors=" + runtime.availableProcessors();
        } catch (Throwable error) {
            return null;
        }
    }

    private static String property(String name) {
        try {
            String value = System.getProperty(name);
            return value == null ? "unknown" : value;
        } catch (RuntimeException error) {
            return "unknown";
        }
    }

    private static String threadName(Thread thread) {
        return thread == null ? null : thread.getName() + " (id=" + thread.threadId() + (thread.isDaemon() ? ", daemon" : "") + ")";
    }

    private static String safeGet(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Throwable error) {
            return "(unavailable: " + error.getClass().getSimpleName() + ")";
        }
    }

    private static void safeLog(String message, Throwable error) {
        try {
            if (error == null) {
                DiagnosticLog.info(message);
            } else {
                DiagnosticLog.error(message, error);
            }
        } catch (Throwable ignored) {
            safeStderr(message);
        }
    }

    private static void safeStderr(String message) {
        try {
            System.err.println(message);
        } catch (Throwable ignored) {
            // 診断処理が元の例外処理を置き換えないことを優先する。
        }
    }

    /**
     * ロード中の診断情報。
     */
    public interface LoadingInfo {
        String currentTask();

        String describe();
    }

    /**
     * 捕捉されない例外を報告してから、元の既定ハンドラーへ処理を委譲する。
     */
    private static final class ReportingHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler delegate;

        private ReportingHandler(Thread.UncaughtExceptionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            try {
                report("Uncaught exception in thread " + thread.getName(), null, error, thread);
            } finally {
                if (delegate != null) {
                    delegate.uncaughtException(thread, error);
                } else {
                    // 既定ハンドラーが無かった場合のJVM標準出力を再現する。
                    // ThreadGroupへ委譲すると、既定ハンドラーであるこのクラスへ戻って再帰するため使用しない。
                    try {
                        System.err.print("Exception in thread \"" + thread.getName() + "\" ");
                        error.printStackTrace(System.err);
                    } catch (Throwable ignored) {
                        // 出力に失敗しても元の終了処理を妨げない。
                    }
                }
            }
        }
    }
}
