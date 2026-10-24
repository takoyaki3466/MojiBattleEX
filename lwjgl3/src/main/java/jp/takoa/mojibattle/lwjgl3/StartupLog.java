package jp.takoa.mojibattle.lwjgl3;

import jp.takoa.mojibattle.log.DiagnosticLog;

import java.nio.file.Path;

/**
 * libGDXの初期化前に使用しても安全な、依存関係のない起動ログ。
 */
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
