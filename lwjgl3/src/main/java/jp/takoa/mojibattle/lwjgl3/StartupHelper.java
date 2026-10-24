package jp.takoa.mojibattle.lwjgl3;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SDL初期化前に必要なWindows向けJoy-Con環境を設定するための起動補助。
 */
final class StartupHelper {
    private static final String BOOTSTRAPPED = "MOJIBATTLE_JOYCON_BOOTSTRAPPED";
    private static final String COMBINE_JOY_CONS = "SDL_JOYSTICK_HIDAPI_COMBINE_JOY_CONS";
    private static final String VERTICAL_JOY_CONS = "SDL_JOYSTICK_HIDAPI_VERTICAL_JOY_CONS";
    private static final String ENABLE_JOY_CONS = "SDL_JOYSTICK_HIDAPI_JOY_CONS";
    private static final String ALLOW_BACKGROUND_EVENTS = "SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS";

    private StartupHelper() {
    }

    /**
     * SDL/Jamepadが読み込まれる前にJoy-Conを1本ずつ横持ちmini-gamepadとして公開する。
     *
     * <p>Javaから親プロセスの環境変数は変更できないため、Windowsでは現在と同じ起動
     * コマンドを必要な環境付きで一度だけ再実行する。jpackageランチャーとGradle JavaExecの
     * どちらでも同じJavaクラスパスとJVM引数を使用する。</p>
     */
    static boolean startNewJvmIfRequired() {
        boolean joyConEnvironmentReady = "0".equals(System.getenv(COMBINE_JOY_CONS)) && "0".equals(System.getenv(VERTICAL_JOY_CONS)) && "1".equals(System.getenv(ENABLE_JOY_CONS));
        boolean diagnosticsEnvironmentReady = !Boolean.getBoolean("mojibattle.controllerDiagnostics") || "1".equals(System.getenv(ALLOW_BACKGROUND_EVENTS));
        if (!isWindows() || "1".equals(System.getenv(BOOTSTRAPPED)) || (joyConEnvironmentReady && diagnosticsEnvironmentReady)) {
            return false;
        }

        List<String> command = new ArrayList<>();
        String packagedLauncher = System.getProperty("jpackage.app-path", "");

        if (packagedLauncher.isBlank()) {

            command.add(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString());
            command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(Lwjgl3Launcher.class.getName());

        } else {
            command.add(packagedLauncher);
        }

        ProcessBuilder child = new ProcessBuilder(command).inheritIO();

        child.environment().put(BOOTSTRAPPED, "1");
        child.environment().put(COMBINE_JOY_CONS, "0");
        child.environment().put(VERTICAL_JOY_CONS, "0");
        child.environment().put(ENABLE_JOY_CONS, "1");

        if (Boolean.getBoolean("mojibattle.controllerDiagnostics")) {
            child.environment().put(ALLOW_BACKGROUND_EVENTS, "1");
        }

        try {
            int exitCode = child.start().waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Joy-Con-enabled child process exited with code " + exitCode);
            }

            return true;

        } catch (IOException error) {
            throw new IllegalStateException("Cannot start Joy-Con-enabled child process", error);

        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Joy-Con-enabled child process", error);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }
}
