package jp.takoa.mojibattle.lwjgl3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** A dependency-free startup log that is safe to use before libGDX is initialized. */
final class StartupLog {
    private static final String APPLICATION_DIRECTORY = "MojiBattleEX";
    private static final String LOG_FILE_NAME = "startup.log";
    private static final Path LOG_FILE = findWritableLogFile();

    private StartupLog() {
    }

    static Path file() {
        return LOG_FILE;
    }

    static void info(String message) {
        write("INFO", message, null);
    }

    static void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private static synchronized void write(String level, String message, Throwable error) {
        try {
            String entry = formatEntry(level, message, error);
            if (LOG_FILE != null) {
                Files.createDirectories(LOG_FILE.getParent());
                Files.writeString(
                        LOG_FILE,
                        entry,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                return;
            }
            System.err.print(entry);
        } catch (Throwable ignored) {
            // Startup diagnostics must never replace the original failure.
            try {
                System.err.println("MojiBattleEX startup logging failed");
            } catch (Throwable alsoIgnored) {
                // There is no safe logging sink left.
            }
        }
    }

    private static String formatEntry(String level, String message, Throwable error) {
        StringBuilder entry = new StringBuilder()
                .append(Instant.now())
                .append(" [")
                .append(level)
                .append("] [")
                .append(Thread.currentThread().getName())
                .append("] ")
                .append(message)
                .append(System.lineSeparator());
        if (error != null) {
            try {
                StringWriter stackTrace = new StringWriter();
                error.printStackTrace(new PrintWriter(stackTrace));
                entry.append(stackTrace);
            } catch (RuntimeException ignored) {
                entry.append("Unable to format stack trace: ")
                        .append(error.getClass().getName())
                        .append(System.lineSeparator());
            }
        }
        return entry.toString();
    }

    private static Path findWritableLogFile() {
        Path localAppData = pathFromEnvironment("LOCALAPPDATA");
        if (localAppData != null) {
            return localAppData.resolve(APPLICATION_DIRECTORY).resolve("logs").resolve(LOG_FILE_NAME);
        }
        Path userHome = pathFromProperty("user.home");
        if (userHome != null) {
            return userHome.resolve("." + APPLICATION_DIRECTORY.toLowerCase()).resolve("logs").resolve(LOG_FILE_NAME);
        }
        Path temporaryDirectory = pathFromProperty("java.io.tmpdir");
        return temporaryDirectory == null
                ? null
                : temporaryDirectory.resolve(APPLICATION_DIRECTORY).resolve(LOG_FILE_NAME);
    }

    private static Path pathFromEnvironment(String name) {
        try {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? null : Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Path pathFromProperty(String name) {
        try {
            String value = System.getProperty(name);
            return value == null || value.isBlank() ? null : Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
