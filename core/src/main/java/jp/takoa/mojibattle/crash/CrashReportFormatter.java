package jp.takoa.mojibattle.crash;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * クラッシュレポートの本文を組み立てる。値の欠落があっても例外を送出せず、
 * 取得できなかった項目は {@code unknown} として出力する。
 */
public final class CrashReportFormatter {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINE = System.lineSeparator();
    private static final int MAX_CAUSE_DEPTH = 32;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private CrashReportFormatter() {
    }

    /**
     * @param data 記録する診断情報
     * @return レポート本文
     */
    public static String format(CrashReportData data) {
        StringBuilder text = new StringBuilder(8192);
        text.append("MojiBattleEX Crash Report").append(LINE);
        text.append("=========================").append(LINE).append(LINE);

        field(text, "Timestamp", safe(() -> TIMESTAMP.format(data.timestamp()) + " (" + data.timestamp().getZone() + ")"));
        field(text, "Game Version", data.gameVersion());
        field(text, "MOD Compatibility Version", data.modCompatibilityVersion());
        field(text, "MOD API Version", data.apiVersion());
        field(text, "Java Version", data.javaVersion());
        field(text, "Java Runtime", data.javaRuntime());
        field(text, "OS", data.os());
        field(text, "Memory", data.memory());
        text.append(LINE);

        field(text, "Game State", data.gameState() == null ? null : data.gameState().name());
        field(text, "Screen", data.screen());
        field(text, "Operation", data.operation());
        field(text, "Current Task", data.currentTask());
        field(text, "Debug Mode", String.valueOf(data.debugMode()));
        field(text, "Thread", data.reportThread());
        field(text, "Error Thread", data.errorThread());
        field(text, "Loaded MODs", data.mods());
        field(text, "Selected Main Characters", data.mainIds());
        field(text, "Selected Support Characters", data.supportIds());
        text.append(LINE);

        Throwable error = data.error();
        text.append("Error:").append(LINE);
        text.append("  ").append(summary(data.summary(), error)).append(LINE).append(LINE);

        text.append("Exception:").append(LINE);
        if (error == null) {
            text.append("  (no exception)").append(LINE);
        } else {
            text.append("  Class: ").append(error.getClass().getName()).append(LINE);
            text.append("  Message: ").append(limit(String.valueOf(error.getMessage()))).append(LINE);
        }
        text.append(LINE);

        text.append("Cause Chain:").append(LINE);
        appendCauseChain(text, error);
        text.append(LINE);

        text.append("Stack Trace:").append(LINE);
        text.append(stackTrace(error)).append(LINE);

        if (data.loadingState() != null && !data.loadingState().isBlank()) {
            text.append("Loading State:").append(LINE).append(data.loadingState()).append(LINE);
        }

        text.append("Report Location:").append(LINE);
        text.append("  ").append(nullToUnknown(data.locationNote())).append(LINE);
        text.append("Diagnostic Log: ").append(nullToUnknown(data.diagnosticLogPath())).append(LINE).append(LINE);

        if (data.recentLog() != null && !data.recentLog().isBlank()) {
            text.append("Recent Diagnostic Log (tail):").append(LINE);
            text.append(data.recentLog());
            if (!data.recentLog().endsWith(LINE)) {
                text.append(LINE);
            }
        }
        return text.toString();
    }

    static String summary(String summary, Throwable error) {
        if (summary != null && !summary.isBlank()) {
            return limit(summary);
        }
        if (error == null) {
            return "unknown error";
        }
        String message = error.getMessage();
        return limit(error.getClass().getSimpleName() + (message == null ? "" : ": " + message));
    }

    private static void appendCauseChain(StringBuilder text, Throwable error) {
        if (error == null) {
            text.append("  (none)").append(LINE);
            return;
        }
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH && seen.add(current)) {
            text.append("  ").append(depth == 0 ? "" : "Caused by: ").append(current.getClass().getName()).append(": ").append(limit(String.valueOf(current.getMessage()))).append(LINE);
            for (Throwable suppressed : current.getSuppressed()) {
                text.append("    Suppressed: ").append(suppressed.getClass().getName()).append(": ").append(limit(String.valueOf(suppressed.getMessage()))).append(LINE);
            }
            current = current.getCause();
            depth++;
        }
        if (current != null && depth >= MAX_CAUSE_DEPTH) {
            text.append("  ... (cause chain truncated)").append(LINE);
        }
    }

    static String stackTrace(Throwable error) {
        if (error == null) {
            return "  (no stack trace)";
        }
        try {
            StringWriter writer = new StringWriter();
            error.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        } catch (Throwable stackTraceFailure) {
            return "  (stack trace unavailable: " + stackTraceFailure.getClass().getName() + ")";
        }
    }

    private static void field(StringBuilder text, String name, String value) {
        text.append(name).append(": ").append(nullToUnknown(value)).append(LINE);
    }

    private static void field(StringBuilder text, String name, List<String> values) {
        field(text, name, values == null ? null : values.isEmpty() ? "(none)" : String.join(", ", values));
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String limit(String value) {
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH) + "... (truncated)";
    }

    private static String safe(ValueSupplier supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException error) {
            return null;
        }
    }

    @FunctionalInterface
    private interface ValueSupplier {
        String get();
    }

    /**
     * レポートへ記録する値。どの値もnullを許容する。
     */
    public record CrashReportData(ZonedDateTime timestamp, String gameVersion, String modCompatibilityVersion, String apiVersion, String javaVersion, String javaRuntime, String os, String memory, GameState gameState, String screen, String operation, String currentTask, boolean debugMode, String reportThread, String errorThread, List<String> mods, List<String> mainIds, List<String> supportIds, String summary, Throwable error, String loadingState, String locationNote, String diagnosticLogPath, String recentLog) {
    }
}
