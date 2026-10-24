package jp.takoa.mojibattle;

import com.badlogic.gdx.Game;
import jp.takoa.mojibattle.screen.ScreenManager;
import jp.takoa.mojibattle.screen.SettingsScreen;
import jp.takoa.mojibattle.screen.TitleScreen;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * libGDXアプリケーションのライフサイクルをゲーム固有の画面管理へ接続するエントリーポイント。
 *
 * <p>生成時に共有コンテキストとタイトル画面を準備し、終了時にはコンテキストが所有する
 * UIリソースを解放する。</p>
 */
public final class GameApplication extends Game {
    private final Supplier<GameContext> contextFactory;
    private final Consumer<String> startupLog;
    private GameContext context;

    public GameApplication(Supplier<GameContext> contextFactory) {
        this(contextFactory, ignored -> {
        });
    }

    public GameApplication(Supplier<GameContext> contextFactory, Consumer<String> startupLog) {
        this.contextFactory = contextFactory;
        this.startupLog = startupLog;
    }

    @Override
    public void create() {
        startupLog.accept("GameApplication.create() started");
        try {
            context = contextFactory.get();

            ScreenManager screens = new ScreenManager(this);
            if (Boolean.getBoolean("mojibattle.controllerDiagnostics")) {
                screens.show(new SettingsScreen(context, screens));
                startupLog.accept("Controller diagnostics settings screen displayed");
            } else {
                screens.show(new TitleScreen(context, screens));
                startupLog.accept("Title screen displayed");
            }
        } catch (RuntimeException | Error error) {
            if (context != null) {
                try {
                    context.close();
                } catch (RuntimeException closeError) {
                    error.addSuppressed(closeError);
                } finally {
                    context = null;
                }
            }
            throw error;
        }
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
