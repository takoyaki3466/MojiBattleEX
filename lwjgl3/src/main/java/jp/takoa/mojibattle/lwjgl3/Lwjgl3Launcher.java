package jp.takoa.mojibattle.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import jp.takoa.mojibattle.GameApplication;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.bootstrap.GameBootstrap;
import jp.takoa.mojibattle.config.GraphicsConfig;
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

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        GraphicsConfig graphics = readGraphicsConfig();
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("MojiBattleEX");
        if (graphics.fullscreen) {
            configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            configuration.setWindowedMode(graphics.windowWidth, graphics.windowHeight);
        }
        configuration.useVsync(true);
        configuration.setForegroundFPS(120);
        ModLoader modLoader = new ModLoader(Path.of(System.getProperty("mojibattle.modsDir", "mods")));
        List<GameMod> mods = discoverMods(modLoader);
        new Lwjgl3Application(
                new GameApplication(() -> createContext(modLoader, mods)),
                configuration);
    }

    private static List<GameMod> discoverMods(ModLoader modLoader) {
        List<GameMod> mods = new ArrayList<>();
        ServiceLoader.load(GameMod.class).forEach(mods::add);
        modLoader.load().stream().map(LoadedMod::instance).forEach(mods::add);
        return List.copyOf(mods);
    }

    private static GameContext createContext(ModLoader modLoader, List<GameMod> mods) {
        try {
            return new GameContext(GameBootstrap.bootstrap(mods, modLoader.resources()), modLoader);
        } catch (RuntimeException error) {
            modLoader.close();
            throw error;
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
}
