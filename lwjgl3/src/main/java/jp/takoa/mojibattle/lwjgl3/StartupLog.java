package jp.takoa.mojibattle.lwjgl3;

import jp.takoa.mojibattle.log.DiagnosticLog;

import java.nio.file.Path;

/** A dependency-free startup log that is safe to use before libGDX is initialized. */
final class StartupLog {
    private StartupLog() {
    }

    static void beginSession() {
        DiagnosticLog.beginSession();
    }

    static Path file() {
        return DiagnosticLog.file();
    }

    static void info(String message) {
        DiagnosticLog.info(message);
    }

    static void error(String message, Throwable error) {
        DiagnosticLog.error(message, error);
    }
}
