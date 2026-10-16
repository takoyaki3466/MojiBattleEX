package jp.takoa.mojibattle.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import jp.takoa.mojibattle.GameApplication;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.bootstrap.GameBootstrap;
import jp.takoa.mojibattle.config.GraphicsConfig;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.modloader.LoadedMod;
import jp.takoa.mojibattle.modloader.ModLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** デスクトップ向けLWJGL3設定を構築し、ゲームアプリケーションを起動するエントリーポイント。 */
public final class Lwjgl3Launcher {
    private static final String GRAPHICS_CONFIG_RESOURCE = "/config/graphics.json";

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) throws Throwable {
        StartupLog.beginSession();
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> StartupLog.error("Uncaught exception in thread " + thread.getName(), error));
        StartupLog.info("Lwjgl3Launcher.main started; log=" + StartupLog.file());
        ModLoader modLoader = null;
        try {
            if (StartupHelper.startNewJvmIfRequired()) {
                StartupLog.info("StartupHelper requested a replacement JVM; current JVM is exiting");
                return;
            }
            StartupLog.info("Loading graphics configuration " + GRAPHICS_CONFIG_RESOURCE);
            GraphicsConfig graphics = readGraphicsConfig();
            StartupLog.info("Graphics configuration loaded");

            Lwjgl3ApplicationConfiguration configuration = createConfiguration(graphics);
            Path modsDirectory = Path.of(System.getProperty("mojibattle.modsDir", "mods"))
                    .toAbsolutePath()
                    .normalize();
            StartupLog.info("MOD directory=" + modsDirectory);
            modLoader = new ModLoader(modsDirectory);
            ModDiscovery discovery = discoverMods(modLoader);
            DiagnosticLog.setLoadedMods(discovery.descriptions());

            ModLoader activeModLoader = modLoader;
            StartupLog.info("Starting LWJGL application");
            new Lwjgl3Application(
                    new GameApplication(
                            () -> createContext(activeModLoader, discovery.mods()),
                            StartupLog::info),
                    configuration);
            StartupLog.info("LWJGL application exited normally");
        } catch (Throwable error) {
            closeAfterFailure(modLoader, error);
            StartupLog.error("Startup failed", error);
            throw error;
        }
    }

    private static Lwjgl3ApplicationConfiguration createConfiguration(GraphicsConfig graphics) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("MojiBattleEX");
        if (graphics.fullscreen) {
            configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            configuration.setWindowedMode(graphics.windowWidth, graphics.windowHeight);
        }
        configuration.useVsync(true);
        configuration.setForegroundFPS(120);
        return configuration;
    }

    private static ModDiscovery discoverMods(ModLoader modLoader) {
        List<GameMod> mods = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        StartupLog.info("Discovering built-in MODs from the application classpath");
        ServiceLoader.load(GameMod.class).forEach(mods::add);
        mods.stream().map(mod -> mod.id() + "@classpath").forEach(descriptions::add);
        StartupLog.info("Built-in MOD discovery complete; count=" + mods.size()
                + ", ids=" + mods.stream().map(GameMod::id).toList());
        StartupLog.info("Calling ModLoader.load() for external MODs");
        List<LoadedMod> externalMods = modLoader.load();
        externalMods.stream().map(LoadedMod::instance).forEach(mods::add);
        externalMods.stream()
                .map(mod -> mod.metadata().id + "@" + mod.metadata().version)
                .forEach(descriptions::add);
        StartupLog.info("ModLoader.load() complete; external count=" + externalMods.size()
                + ", external ids=" + externalMods.stream().map(mod -> mod.metadata().id).toList()
                + ", total count=" + mods.size());
        return new ModDiscovery(List.copyOf(mods), List.copyOf(descriptions));
    }

    private static GameContext createContext(ModLoader modLoader, List<GameMod> mods) {
        StartupLog.info("GameBootstrap.bootstrap() started");
        try {
            var bootstrap = GameBootstrap.bootstrap(mods, modLoader.resources());
            StartupLog.info("GameBootstrap.bootstrap() completed");
            StartupLog.info("GameContext initialization started");
            GameContext context = new GameContext(bootstrap, modLoader);
            StartupLog.info("GameContext initialization completed");
            return context;
        } catch (Throwable error) {
            closeAfterFailure(modLoader, error);
            throw error;
        }
    }

    private static void closeAfterFailure(ModLoader modLoader, Throwable failure) {
        if (modLoader == null) {
            return;
        }
        try {
            modLoader.close();
        } catch (Throwable closeError) {
            try {
                failure.addSuppressed(closeError);
            } catch (RuntimeException ignored) {
                StartupLog.error("Unable to attach MOD loader cleanup failure", closeError);
            }
        }
    }

    private static GraphicsConfig readGraphicsConfig() {
        InputStream stream = Lwjgl3Launcher.class.getResourceAsStream(GRAPHICS_CONFIG_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException(
                    "Cannot load startup graphics config from application resources: "
                            + GRAPHICS_CONFIG_RESOURCE);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Json().fromJson(GraphicsConfig.class, reader);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException(
                    "Cannot parse startup graphics config: " + GRAPHICS_CONFIG_RESOURCE,
                    error);
        }
    }

    private record ModDiscovery(List<GameMod> mods, List<String> descriptions) {
    }
}
