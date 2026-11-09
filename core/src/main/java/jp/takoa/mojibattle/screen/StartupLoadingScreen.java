package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.crash.CrashReporter;
import jp.takoa.mojibattle.debug.DebugSettings;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.loading.LoadTask;
import jp.takoa.mojibattle.loading.LoadTaskState;
import jp.takoa.mojibattle.loading.LoadingDebugText;
import jp.takoa.mojibattle.loading.LoadingTracker;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.startup.StartupLoader;
import jp.takoa.mojibattle.ui.UiFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ゲーム起動直後に表示し、{@link StartupLoader} の実際の初期化処理と連動した進捗を示すロード画面。
 *
 * <p>この時点では共有Skin（{@code GameContext}）がまだ存在しないため、表示に必要な文字だけを収録した
 * 小さなフォントをこの画面で生成し、画面の破棄時に解放する。配色は既定テーマのパネルと進捗バーに合わせる。</p>
 *
 * <p>初期化は1フレームあたり一定時間だけ進めるため、ロード中も描画が継続する。全タスクが成功するまで最初の画面へは
 * 遷移せず、失敗した場合はクラッシュレポートを保存し、保存先と終了方法を表示する。</p>
 */
public final class StartupLoadingScreen extends ScreenAdapter {
    /**
     * 起動時の文言に使う言語。設定ファイルの読込み前に表示するため、同梱の既定言語を使う。
     */
    static final String LANGUAGE = "ja_jp";
    /** 1フレームで初期化処理に使う時間の目安。60FPSの1フレーム（約16.7ms）に描画の余裕を残す。 */
    private static final long WORK_BUDGET_NANOS = 10_000_000L;
    private static final float DEBUG_REFRESH_SECONDS = .1f;
    private static final List<String> FIXED_KEYS = List.of("startup.title", "startup.complete", "startup.error.title", "startup.error.message", "startup.error.no_report", "startup.error.exit", "startup.error.transition");

    private static final Color BACKGROUND = new Color(.015f, .075f, .052f, 1f);
    private static final Color PANEL = new Color(.025f, .14f, .095f, .96f);
    private static final Color PANEL_BORDER = new Color(.12f, .68f, .34f, 1f);
    private static final Color BAR_BACKGROUND = new Color(.055f, .20f, .12f, .98f);
    private static final Color BAR_FILL = new Color(.30f, .94f, .40f, 1f);
    private static final Color ERROR = new Color(1f, .45f, .40f, 1f);
    private static final Color DEBUG_TEXT = new Color(.80f, 1f, .84f, 1f);

    private final StartupLoader loader;
    private final LoadingTracker tracker;
    private final Consumer<StartupLoader.Result> onReady;
    private final LocalizationManager text;
    private final CrashReporter.LoadingInfo crashInfo;

    private final SpriteBatch batch;
    private final Texture white;
    private final GlyphLayout layout = new GlyphLayout();
    private final String baseCharacters;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont debugFont;

    private String title;
    private String message = "";
    private String debugDetails = "";
    private boolean firstFrameDrawn;
    private boolean readyToStart;
    private boolean handedOver;
    private boolean errorShown;
    private boolean disposed;
    private float debugRefreshTimer;

    /**
     * @param loader  実行する起動処理。この画面が所有し、破棄時に未受渡しの成果物を解放する
     * @param onReady 全タスク成功後に成果物を受け取り、最初の画面へ切り替える処理
     */
    public StartupLoadingScreen(StartupLoader loader, Consumer<StartupLoader.Result> onReady) {
        this.loader = loader;
        this.tracker = loader.tracker();
        this.onReady = onReady;
        text = new LocalizationManager(LANGUAGE);
        crashInfo = new CrashReporter.LoadingInfo() {
            @Override
            public String currentTask() {
                LoadTask task = tracker.current();
                return task == null ? "none" : task.debugName();
            }

            @Override
            public String describe() {
                return "Startup loading" + System.lineSeparator() + tracker.describe();
            }
        };

        StringBuilder characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
        for (String key : FIXED_KEYS) {
            characters.append(text.text(key));
        }
        for (LoadTask task : tracker.tasks()) {
            characters.append(text.text(task.messageKey(), 0, 0));
        }
        baseCharacters = characters.toString();

        batch = new SpriteBatch(256);
        Texture pixel = null;
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            try {
                pixmap.setColor(Color.WHITE);
                pixmap.fill();
                pixel = new Texture(pixmap);
            } finally {
                pixmap.dispose();
            }
            white = pixel;
            generateFonts(baseCharacters);
        } catch (RuntimeException | Error error) {
            disposeFonts();
            batch.dispose();
            if (pixel != null) {
                pixel.dispose();
            }
            throw error;
        }
        title = text.text("startup.title");
        refreshText();
    }

    @Override
    public void show() {
        CrashReporter.setLoadingInfo(crashInfo);
        Gdx.input.setInputProcessor(null);
        DiagnosticLog.info("Startup loading screen displayed");
    }

    @Override
    public void render(float delta) {
        if (handedOver || disposed) {
            return;
        }
        // 最初のフレームは処理を始めずに描画し、起動直後に必ずロード画面が見えるようにする。
        if (firstFrameDrawn && !errorShown) {
            if (readyToStart) {
                handOver();
                if (handedOver) {
                    return;
                }
            } else {
                boolean done = loader.update(WORK_BUDGET_NANOS);
                if (loader.failure() != null) {
                    showLoadingError(loader.failure());
                } else if (done) {
                    // 100%を1フレーム表示してから遷移する。
                    readyToStart = true;
                }
            }
        }
        debugRefreshTimer -= delta;
        refreshText();
        draw();
        firstFrameDrawn = true;
    }

    private void handOver() {
        StartupLoader.Result result = loader.takeResult();
        try {
            handedOver = true;
            onReady.accept(result);
        } catch (RuntimeException | LinkageError error) {
            // ScreenManagerが失敗した画面を破棄し、このロード画面へ戻している。コンテキストは受け取り側が保持する。
            handedOver = false;
            DiagnosticLog.error("First screen transition failed after startup loading", error);
            Optional<Path> report = CrashReporter.report("Startup screen transition", "First screen could not be shown after startup loading completed", error);
            showError(text.text("startup.error.transition"), report);
        }
    }

    private void showLoadingError(Throwable error) {
        LoadTask failed = tracker.failedTask();
        String taskName = failed == null ? "unknown" : failed.debugName();
        Optional<Path> report = CrashReporter.report("Startup loading: " + taskName, "Startup failed at task '" + taskName + "'", error);
        showError(failed == null ? taskName : taskMessage(failed), report);
    }

    private void showError(String failedTask, Optional<Path> report) {
        errorShown = true;
        title = text.text("startup.error.title");
        String location = report.map(Path::toString).orElse(null);
        message = (location == null ? text.text("startup.error.no_report", failedTask, String.valueOf(DiagnosticLog.file())) : text.text("startup.error.message", failedTask, location)) + "\n" + text.text("startup.error.exit");
        debugRefreshTimer = 0;
        try {
            // 保存先のパスなど、起動時フォントに含まれない文字を表示できるよう生成し直す。
            disposeFonts();
            generateFonts(baseCharacters + message);
        } catch (RuntimeException fontError) {
            DiagnosticLog.error("Cannot regenerate startup font for the error message", fontError);
        }
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.SPACE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Gdx.app.exit();
                return true;
            }
        });
    }

    private void refreshText() {
        if (!errorShown) {
            LoadTask current = tracker.current();
            message = current == null ? text.text("startup.complete") : taskMessage(current);
        }
        boolean debug = DebugSettings.enabled();
        if (debug && debugRefreshTimer <= 0) {
            debugRefreshTimer = DEBUG_REFRESH_SECONDS;
            debugDetails = LoadingDebugText.format(tracker);
        } else if (!debug) {
            debugDetails = "";
        }
    }

    private String taskMessage(LoadTask task) {
        if (task.totalUnits() > 0) {
            int shown = task.state() == LoadTaskState.DONE ? task.totalUnits() : Math.min(task.totalUnits(), task.completedUnits() + 1);
            return text.text(task.messageKey(), shown, task.totalUnits());
        }
        return text.text(task.messageKey(), 0, 0);
    }

    private void draw() {
        Gdx.gl.glClearColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (titleFont == null || bodyFont == null || debugFont == null) {
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);

        float panelWidth = Math.min(width - 32f, Math.max(480f, width * .62f));
        float inner = panelWidth - 64f;
        boolean debug = !debugDetails.isEmpty();

        layout.setText(bodyFont, message, Color.WHITE, inner, Align.center, true);
        float messageHeight = Math.max(layout.height, bodyFont.getLineHeight());
        float debugHeight = 0f;
        if (debug) {
            layout.setText(debugFont, debugDetails, DEBUG_TEXT, inner, Align.left, true);
            debugHeight = layout.height + 20f;
        }
        float panelHeight = 32f + titleFont.getLineHeight() + 16f + messageHeight + 24f + 26f + 12f + bodyFont.getLineHeight() + debugHeight + 28f;
        float panelX = (width - panelWidth) / 2f;
        float panelY = Math.max(8f, (height - panelHeight) / 2f);
        float left = panelX + 32f;

        batch.begin();
        fill(panelX - 2f, panelY - 2f, panelWidth + 4f, panelHeight + 4f, PANEL_BORDER);
        fill(panelX, panelY, panelWidth, panelHeight, PANEL);

        float y = panelY + panelHeight - 32f;
        titleFont.setColor(errorShown ? ERROR : Color.WHITE);
        titleFont.draw(batch, title, left, y, inner, Align.center, false);
        y -= titleFont.getLineHeight() + 16f;

        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(batch, message, left, y, inner, Align.center, true);
        y -= messageHeight + 24f;

        float barHeight = 26f;
        fill(left, y - barHeight, inner, barHeight, BAR_BACKGROUND);
        float progress = Math.max(0f, Math.min(1f, tracker.progress()));
        if (progress > 0f) {
            fill(left, y - barHeight, inner * progress, barHeight, BAR_FILL);
        }
        y -= barHeight + 12f;

        bodyFont.draw(batch, tracker.percent() + "%", left, y, inner, Align.center, false);
        y -= bodyFont.getLineHeight();

        if (debug) {
            y -= 20f;
            debugFont.setColor(DEBUG_TEXT);
            debugFont.draw(batch, debugDetails, left, y, inner, Align.left, true);
        }
        batch.end();
    }

    private void fill(float x, float y, float width, float height, Color color) {
        batch.setColor(color);
        batch.draw(white, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private void generateFonts(String characters) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(UiFactory.fontFile());
        try {
            titleFont = generator.generateFont(parameter(40, characters, 1.5f));
            bodyFont = generator.generateFont(parameter(26, characters, 0f));
            // デバッグ表示はタスク名・時間などの英数字のみ。
            debugFont = generator.generateFont(parameter(17, FreeTypeFontGenerator.DEFAULT_CHARS, 0f));
        } catch (RuntimeException | Error error) {
            disposeFonts();
            throw error;
        } finally {
            generator.dispose();
        }
    }

    private static FreeTypeFontGenerator.FreeTypeFontParameter parameter(int size, String characters, float border) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = unique(characters);
        parameter.borderWidth = border;
        parameter.borderColor = new Color(.04f, .30f, .16f, 1f);
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        return parameter;
    }

    private static String unique(String input) {
        StringBuilder result = new StringBuilder();
        input.codePoints().distinct().forEach(result::appendCodePoint);
        return result.toString();
    }

    /** 起動処理または最初の画面への切替えに失敗し、エラーを表示しているか。 */
    public boolean failed() {
        return errorShown;
    }

    @Override
    public void hide() {
        if (errorShown) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        try {
            loader.dispose();
        } finally {
            CrashReporter.setLoadingInfo(null);
            disposeResources();
        }
    }

    private void disposeResources() {
        disposeFonts();
        batch.dispose();
        if (white != null) {
            white.dispose();
        }
    }

    private void disposeFonts() {
        if (titleFont != null) {
            titleFont.dispose();
            titleFont = null;
        }
        if (bodyFont != null) {
            bodyFont.dispose();
            bodyFont = null;
        }
        if (debugFont != null) {
            debugFont.dispose();
            debugFont = null;
        }
    }
}
