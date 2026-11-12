package jp.takoa.mojibattle;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import jp.takoa.mojibattle.crash.CrashReporter;
import jp.takoa.mojibattle.crash.GameState;
import jp.takoa.mojibattle.debug.DebugOverlay;
import jp.takoa.mojibattle.debug.DebugSettings;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.screen.ScreenManager;
import jp.takoa.mojibattle.screen.SettingsScreen;
import jp.takoa.mojibattle.screen.StartupLoadingScreen;
import jp.takoa.mojibattle.screen.TitleScreen;
import jp.takoa.mojibattle.startup.StartupLoader;
import jp.takoa.mojibattle.startup.StartupSource;
import jp.takoa.mojibattle.ui.UiThemeManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * libGDXアプリケーションのライフサイクルをゲーム固有の画面管理へ接続するエントリーポイント。
 *
 * <p>{@link StartupSource} を受け取る場合は、生成時に起動ロード画面を表示し、MODの登録、データの読込み、
 * フォント生成などを {@link StartupLoader} でフレームごとに進めてからタイトル画面へ切り替える。
 * {@link Supplier} を受け取る従来の形では、生成時に共有コンテキストとタイトル画面を一括で準備する。
 * 終了時にはコンテキストが所有するUIリソースを解放する。</p>
 *
 * <p>全画面の描画後にデバッグ用オーバーレイを重ね、描画中に捕捉されなかった例外はクラッシュレポートへ記録してから
 * 従来どおり上位へ送出する。</p>
 */
public final class GameApplication extends Game {
    private final Supplier<GameContext> contextFactory;
    private final StartupSource startupSource;
    private final Consumer<String> startupLog;
    private GameContext context;
    private StartupLoadingScreen startupScreen;
    private boolean startupFailed;
    private final DebugOverlay debugOverlay = new DebugOverlay();

    public GameApplication(Supplier<GameContext> contextFactory) {
        this(contextFactory, ignored -> {
        });
    }

    public GameApplication(Supplier<GameContext> contextFactory, Consumer<String> startupLog) {
        this.contextFactory = contextFactory;
        this.startupSource = null;
        this.startupLog = startupLog;
    }

    /**
     * 起動ロード画面を表示しながら初期化する。
     *
     * @param startupSource MODの供給元
     * @param startupLog    起動ログの出力先
     */
    public GameApplication(StartupSource startupSource, Consumer<String> startupLog) {
        this.contextFactory = null;
        this.startupSource = startupSource;
        this.startupLog = startupLog;
    }

    @Override
    public void create() {
        startupLog.accept("GameApplication.create() started");
        CrashReporter.setGameState(GameState.STARTUP);
        try {
            boolean debugMode = DebugSettings.load(Gdx.app.getPreferences(UiThemeManager.PREFERENCES_NAME));
            startupLog.accept("Debug mode=" + debugMode);
            ScreenManager screens = new ScreenManager(this);
            if (startupSource != null) {
                StartupLoader loader = new StartupLoader(startupSource, created -> firstScreen(created, screens));
                StartupLoadingScreen loading;
                try {
                    loading = new StartupLoadingScreen(loader, result -> finishStartup(result, screens));
                } catch (RuntimeException | Error error) {
                    loader.dispose();
                    throw error;
                }
                startupScreen = loading;
                screens.show(loading);
                startupLog.accept("Startup loading screen displayed");
                return;
            }
            context = contextFactory.get();
            showFirstScreen(firstScreen(context, screens), screens);
        } catch (RuntimeException | Error error) {
            CrashReporter.report("GameApplication.create", error);
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

    private Screen firstScreen(GameContext created, ScreenManager screens) {
        if (Boolean.getBoolean("mojibattle.controllerDiagnostics")) {
            return new SettingsScreen(created, screens);
        }
        return new TitleScreen(created, screens);
    }

    /**
     * 起動ロードの成果物を受け取り、最初の画面へ切り替える。コンテキストの所有権はこのクラスへ移る。
     */
    private void finishStartup(StartupLoader.Result result, ScreenManager screens) {
        context = result.context();
        showFirstScreen(result.firstScreen(), screens);
        // ScreenManagerが起動ロード画面を破棄した。
        startupScreen = null;
    }

    private void showFirstScreen(Screen first, ScreenManager screens) {
        screens.show(first);
        if (first instanceof SettingsScreen) {
            startupLog.accept("Controller diagnostics settings screen displayed");
        } else {
            startupLog.accept("Title screen displayed");
        }
    }

    @Override
    public void render() {
        try {
            super.render();
            debugOverlay.render();
        } catch (RuntimeException | Error error) {
            // 継続の安全性を保証できないため、記録後は既存の異常終了処理へ委ねる。
            CrashReporter.report("render: " + DiagnosticLog.currentScreen(), error);
            throw error;
        }
    }

    @Override
    public void resize(int width, int height) {
        try {
            super.resize(width, height);
        } catch (RuntimeException | Error error) {
            CrashReporter.report("resize: " + DiagnosticLog.currentScreen(), error);
            throw error;
        }
    }

    /**
     * 起動ロードが失敗したまま終了したか。ランチャーが終了状態の判断に使う。
     */
    public boolean startupFailed() {
        return startupFailed || startupScreen != null && startupScreen.failed();
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            try {
                // 起動ロード中に終了した場合は、ローダーが所有するMODランタイム等をここで解放する。
                if (startupScreen != null) {
                    StartupLoadingScreen loading = startupScreen;
                    startupScreen = null;
                    startupFailed = loading.failed();
                    loading.dispose();
                }
            } finally {
                try {
                    debugOverlay.dispose();
                } finally {
                    if (context != null) {
                        context.close();
                    }
                }
            }
        }
    }
}
