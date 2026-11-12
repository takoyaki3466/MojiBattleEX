package jp.takoa.mojibattle.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.controllers.desktop.JamepadControllerManager;
import com.badlogic.gdx.utils.Json;
import com.studiohartman.jamepad.Configuration;
import jp.takoa.mojibattle.GameApplication;
import jp.takoa.mojibattle.config.GraphicsConfig;
import jp.takoa.mojibattle.info.InstalledMod;
import jp.takoa.mojibattle.info.InstalledMods;
import jp.takoa.mojibattle.crash.CrashReporter;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.modloader.LoadedMod;
import jp.takoa.mojibattle.modloader.ModLoader;
import jp.takoa.mojibattle.modloader.ModMetadata;
import jp.takoa.mojibattle.resource.ResourceManager;
import jp.takoa.mojibattle.startup.StartupSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * デスクトップ向けLWJGL3設定を構築し、ゲームアプリケーションを起動するエントリーポイント。
 */
public final class Lwjgl3Launcher {
    private static final String GRAPHICS_CONFIG_RESOURCE = "/config/graphics.json";
    private static final String MOD_METADATA_RESOURCE = "META-INF/mojibattle/mod.json";

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) throws Throwable {
        StartupLog.beginSession();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> StartupLog.error("Uncaught exception in thread " + thread.getName(), error));
        // 既存の診断ログ出力を残したまま、その前段でクラッシュレポートを生成する。
        CrashReporter.installUncaughtExceptionHandler();
        CrashReporter.setGameVersion(Lwjgl3Launcher.class.getPackage().getImplementationVersion());

        StartupLog.info("Lwjgl3Launcher.main started; log=" + StartupLog.file());
        StartupLog.info("Crash report directory=" + CrashReporter.location().primary() + ", fallbacks=" + CrashReporter.location().fallbacks());

        ModLoader modLoader = null;
        try {
            if (StartupHelper.startNewJvmIfRequired()) {
                StartupLog.info("StartupHelper requested a replacement JVM; current JVM is exiting");
                return;
            }

            configureControllerBackend();
            StartupLog.info("Loading graphics configuration " + GRAPHICS_CONFIG_RESOURCE);

            GraphicsConfig graphics = readGraphicsConfig();
            StartupLog.info("Graphics configuration loaded");

            Lwjgl3ApplicationConfiguration configuration = createConfiguration(graphics);
            Path modsDirectory = Path.of(System.getProperty("mojibattle.modsDir", "mods")).toAbsolutePath().normalize();
            StartupLog.info("MOD directory=" + modsDirectory);

            modLoader = new ModLoader(modsDirectory);
            // MODの探索はウィンドウ表示後、起動ロード画面の最初のタスクとして実行する。
            GameApplication application = new GameApplication(new LauncherStartupSource(modLoader), StartupLog::info);
            StartupLog.info("Starting LWJGL application");
            new Lwjgl3Application(application, configuration);
            if (application.startupFailed()) {
                // 起動ロード画面でエラーとレポートの保存先を表示し、利用者が閉じた。従来どおり異常終了として扱う。
                StartupLog.info("LWJGL application exited after a startup failure");
                System.exit(1);
            }
            StartupLog.info("LWJGL application exited normally");
        } catch (Throwable error) {
            CrashReporter.report("Lwjgl3Launcher.main", error);
            closeAfterFailure(modLoader, error);
            StartupLog.error("Startup failed", error);
            throw error;
        }
    }

    /**
     * Controllers初回参照より前に、SDL/Jamepadの管理台数と入力方式を固定する。
     */
    private static void configureControllerBackend() {
        Configuration controllerConfiguration = new Configuration();
        // Joy-Con以外が先に列挙されても、4本のJoy-Conを発見できるだけの探索枠を確保する。
        // 実際のプレイヤー割り当て上限はJoyConInputManagerのP1〜P4である。
        controllerConfiguration.maxNumControllers = 16;
        controllerConfiguration.useRawInput = false;
        JamepadControllerManager.jamepadConfiguration = controllerConfiguration;
        StartupLog.info("Controller backend configured; discoverySlots=16, assignedJoyCons=4, rawInput=false");
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
        List<InstalledMod> installed = new ArrayList<>();

        StartupLog.info("Discovering built-in MODs from the application classpath");
        ServiceLoader.load(GameMod.class).forEach(mods::add);
        mods.stream().map(mod -> mod.id() + "@classpath").forEach(descriptions::add);
        Map<String, ModMetadata> builtInMetadata = readBuiltInMetadata();
        for (GameMod mod : mods) {
            ModMetadata metadata = builtInMetadata.get(mod.id());
            installed.add(new InstalledMod(mod.id(), metadata == null ? null : metadata.name, metadata == null ? null : metadata.version, InstalledMod.Source.BUILT_IN, null));
        }

        StartupLog.info("Built-in MOD discovery complete; count=" + mods.size() + ", ids=" + mods.stream().map(GameMod::id).toList());
        StartupLog.info("Calling ModLoader.load() for external MODs");

        List<LoadedMod> externalMods = modLoader.load();
        externalMods.stream().map(LoadedMod::instance).forEach(mods::add);
        externalMods.stream().map(mod -> mod.metadata().id + "@" + mod.metadata().version).forEach(descriptions::add);
        for (LoadedMod mod : externalMods) {
            installed.add(new InstalledMod(mod.metadata().id, mod.metadata().name, mod.metadata().version, InstalledMod.Source.EXTERNAL, mod.jar().getFileName().toString()));
        }

        StartupLog.info("ModLoader.load() complete; external count=" + externalMods.size() + ", external ids=" + externalMods.stream().map(mod -> mod.metadata().id).toList() + ", total count=" + mods.size());
        return new ModDiscovery(List.copyOf(mods), List.copyOf(descriptions), List.copyOf(installed));
    }

    /**
     * クラスパス上の同梱MODの {@code META-INF/mojibattle/mod.json} を読み、IDごとの名前とバージョンを返す。
     * 表示用の補助情報のため、読めないファイルは診断ログへ記録して無視する（MODの読込み自体には影響しない）。
     */
    private static Map<String, ModMetadata> readBuiltInMetadata() {
        Map<String, ModMetadata> result = new HashMap<>();
        try {
            Enumeration<URL> resources = Lwjgl3Launcher.class.getClassLoader().getResources(MOD_METADATA_RESOURCE);
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                    ModMetadata metadata = json.fromJson(ModMetadata.class, reader);
                    if (metadata != null && metadata.id != null) {
                        result.putIfAbsent(metadata.id, metadata);
                    }
                } catch (IOException | RuntimeException error) {
                    StartupLog.error("Cannot read built-in MOD metadata for the information screen: " + url, error);
                }
            }
        } catch (IOException error) {
            StartupLog.error("Cannot list built-in MOD metadata for the information screen", error);
        }
        return result;
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
            throw new IllegalStateException("Cannot load startup graphics config from application resources: " + GRAPHICS_CONFIG_RESOURCE);
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Json().fromJson(GraphicsConfig.class, reader);

        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot parse startup graphics config: " + GRAPHICS_CONFIG_RESOURCE, error);
        }
    }

    private record ModDiscovery(List<GameMod> mods, List<String> descriptions, List<InstalledMod> installed) {
    }

    /**
     * 起動ロード画面へMODを供給する。ModLoaderの所有権は起動処理を経てGameContextへ移る。
     */
    private static final class LauncherStartupSource implements StartupSource {
        private final ModLoader modLoader;

        private LauncherStartupSource(ModLoader modLoader) {
            this.modLoader = modLoader;
        }

        @Override
        public List<GameMod> discoverMods() {
            ModDiscovery discovery = Lwjgl3Launcher.discoverMods(modLoader);
            DiagnosticLog.setLoadedMods(discovery.descriptions());
            InstalledMods.set(discovery.installed());
            return discovery.mods();
        }

        @Override
        public ResourceManager resources() {
            return modLoader.resources();
        }

        @Override
        public AutoCloseable runtime() {
            return modLoader;
        }
    }
}
