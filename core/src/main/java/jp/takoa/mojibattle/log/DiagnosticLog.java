package jp.takoa.mojibattle.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/** libGDX初期化前から利用できる、直近実行用の障害診断ログ。 */
public final class DiagnosticLog {
    private static final Path LOG_FILE = findLogFile();
    private static volatile String currentScreen = "not-initialized";
    private static volatile List<String> loadedMods = List.of();
    private static volatile Selection selection = Selection.empty();

    private DiagnosticLog() {
    }

    public static Path file() {
        return LOG_FILE;
    }

    /** 新しい起動の先頭で、前回分を残さず最新実行のログへ切り替える。 */
    public static synchronized void beginSession() {
        if (LOG_FILE == null) {
            return;
        }
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Throwable ignored) {
            safeStderr("MojiBattleEX diagnostic log initialization failed\n");
        }
    }

    public static void setCurrentScreen(String screenName) {
        currentScreen = screenName == null ? "unknown" : screenName;
    }

    public static void setLoadedMods(List<String> mods) {
        loadedMods = mods == null ? List.of() : List.copyOf(mods);
    }

    public static void setSelection(List<String> mainIds, List<String> supportIds,
                                    List<String> glyphTextures, List<String> projectileTextures) {
        selection = new Selection(mainIds, supportIds, glyphTextures, projectileTextures);
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable error) {
        write("ERROR", message, error);
    }

    private static synchronized void write(String level, String message, Throwable error) {
        try {
            String entry = formatEntry(level, message, error);
            if (LOG_FILE != null) {
                Files.createDirectories(LOG_FILE.getParent());
                Files.writeString(LOG_FILE, entry, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return;
            }
            safeStderr(entry);
        } catch (Throwable ignored) {
            safeStderr("MojiBattleEX diagnostic logging failed\n");
        }
    }

    private static String formatEntry(String level, String message, Throwable error) {
        Selection snapshot = selection;
        StringBuilder entry = new StringBuilder()
                .append(Instant.now()).append(" [").append(level).append("] [")
                .append(Thread.currentThread().getName()).append("] ").append(message)
                .append(System.lineSeparator())
                .append("screen=").append(currentScreen).append(System.lineSeparator())
                .append("mods=").append(loadedMods).append(System.lineSeparator())
                .append("mainIds=").append(snapshot.mainIds()).append(System.lineSeparator())
                .append("supportIds=").append(snapshot.supportIds()).append(System.lineSeparator())
                .append("glyphTextures=").append(snapshot.glyphTextures()).append(System.lineSeparator())
                .append("projectileTextures=").append(snapshot.projectileTextures()).append(System.lineSeparator());
        if (error != null) {
            StringWriter stackTrace = new StringWriter();
            error.printStackTrace(new PrintWriter(stackTrace));
            entry.append(stackTrace);
        }
        return entry.toString();
    }

    private static void safeStderr(String message) {
        try {
            System.err.print(message);
        } catch (Throwable ignored) {
            // 診断処理が元の例外を置き換えないことを優先する。
        }
    }

    private static Path findLogFile() {
        Path localAppData = pathFromEnvironment("LOCALAPPDATA");
        if (localAppData != null) {
            return localAppData.resolve("MojiBattleEX").resolve("logs").resolve("latest.log");
        }
        Path userHome = pathFromProperty("user.home");
        if (userHome != null) {
            return userHome.resolve(".mojibattleex").resolve("logs").resolve("latest.log");
        }
        Path temporary = pathFromProperty("java.io.tmpdir");
        return temporary == null ? null : temporary.resolve("MojiBattleEX").resolve("latest.log");
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

    private record Selection(List<String> mainIds, List<String> supportIds,
                             List<String> glyphTextures, List<String> projectileTextures) {
        private Selection {
            mainIds = copy(mainIds);
            supportIds = copy(supportIds);
            glyphTextures = copy(glyphTextures);
            projectileTextures = copy(projectileTextures);
        }

        private static Selection empty() {
            return new Selection(List.of(), List.of(), List.of(), List.of());
        }

        private static List<String> copy(List<String> values) {
            return values == null ? List.of() : List.copyOf(values);
        }
    }
}
