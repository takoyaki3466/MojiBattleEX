package jp.takoa.mojibattle.startup;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.bootstrap.BootstrapProcess;
import jp.takoa.mojibattle.bootstrap.GameBootstrap;
import jp.takoa.mojibattle.loading.LoadTask;
import jp.takoa.mojibattle.loading.LoadTaskState;
import jp.takoa.mojibattle.loading.LoadingTracker;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.registry.RegistrySnapshot;
import jp.takoa.mojibattle.ui.AsyncFontGenerator;
import jp.takoa.mojibattle.ui.UiFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ゲーム起動時の初期化を、フレームごとに少しずつ進める。
 *
 * <p>従来 {@code GameApplication.create()} の中で一括実行していた処理を次のタスクに分け、状態を
 * {@link LoadingTracker} へ記録する。各処理の内容と順序は従来と同じである。</p>
 * <ol>
 *     <li>MODの探索（標準MODと外部MOD JAR）</li>
 *     <li>MODの登録（{@link GameMod#register}。1フレームに1MOD）</li>
 *     <li>ゲームデータの読込み（登録イベントの発行、設定JSONの解決）</li>
 *     <li>翻訳データの読込みと検証</li>
 *     <li>キャラクターの登録（保存済みカスタムキャラクターを含む）</li>
 *     <li>能力の初期化とRegistryの凍結</li>
 *     <li>MODの初期化（{@link GameMod#initialize}。1フレームに1MOD）</li>
 *     <li>フォントの生成（ラスタライズはワーカースレッド、Texture化はGLスレッドで1フォントずつ）</li>
 *     <li>UIリソースの準備（テーマ画像のTexture化とSkinの組立て。画像のデコードは起動直後からワーカースレッドで行う）</li>
 *     <li>ゲーム共有サービスの準備（入力デバイス管理、効果音）</li>
 *     <li>最初の画面の準備</li>
 * </ol>
 * <p>MODのコード、OpenGL、ゲーム状態はGLスレッドでのみ扱う。ワーカースレッドはフォントのラスタライズと
 * テーマ背景画像のデコード（いずれもOpenGLを使わないPixmapへの処理）だけを行う。画像のデコードは起動直後から
 * MODの登録などと並行して進める。フォントに収録する文字は全段階のブートストラップ完了後に確定するため、
 * ラスタライズはその時点で開始する。</p>
 */
public final class StartupLoader implements Disposable {
    private final StartupSource source;
    private final Function<GameContext, Screen> firstScreenFactory;
    private final LoadingTracker tracker;

    private final LoadTask discoveryTask;
    private final LoadTask registerTask;
    private final LoadTask dataTask;
    private final LoadTask localizationTask;
    private final LoadTask charactersTask;
    private final LoadTask abilitiesTask;
    private final LoadTask initializeTask;
    private final LoadTask fontsTask;
    private final LoadTask uiTask;
    private final LoadTask contextTask;
    private final LoadTask screenTask;

    private AutoCloseable runtime;
    private BootstrapProcess process;
    private RegistrySnapshot snapshot;
    private AsyncFontGenerator fontGenerator;
    private ImagePreloader images;
    private final Map<String, BitmapFont> fonts = new LinkedHashMap<>();
    private Skin skin;
    private GameContext context;
    private Screen firstScreen;

    private Throwable failure;
    private boolean resultTaken;
    private boolean disposed;

    public StartupLoader(StartupSource source, Function<GameContext, Screen> firstScreenFactory) {
        this(source, firstScreenFactory, new LoadingTracker());
    }

    StartupLoader(StartupSource source, Function<GameContext, Screen> firstScreenFactory, LoadingTracker tracker) {
        this.source = source;
        this.firstScreenFactory = firstScreenFactory;
        this.tracker = tracker;

        // 重みはヘッドレス環境での計測比に基づく。フォント生成が起動時間の大半を占める。
        discoveryTask = tracker.add("MOD Discovery", "startup.task.discover", 1f);
        registerTask = tracker.add("MOD Registration", "startup.task.register", 1f);
        dataTask = tracker.add("Game Data", "startup.task.data", 2f);
        localizationTask = tracker.add("Localization", "startup.task.localization", 1f);
        charactersTask = tracker.add("Character Registry", "startup.task.characters", 1f);
        abilitiesTask = tracker.add("Abilities", "startup.task.abilities", 1f);
        initializeTask = tracker.add("MOD Initialization", "startup.task.initialize", 1f);
        fontsTask = tracker.add("Font Generation", "startup.task.fonts", 14f);
        uiTask = tracker.add("UI Resources", "startup.task.ui", 1f);
        contextTask = tracker.add("Game Services", "startup.task.services", 1f);
        screenTask = tracker.add("First Screen", "startup.task.screen", 1f);
    }

    public LoadingTracker tracker() {
        return tracker;
    }

    public Throwable failure() {
        return failure;
    }

    /**
     * 指定時間を目安に初期化を進める。1件の処理は途中で中断しないため、目安を超える場合がある。
     *
     * @return 全タスクが成功していればtrue
     */
    public boolean update(long budgetNanos) {
        if (disposed || failure != null || tracker.finished()) {
            return tracker.finished();
        }
        long start = System.nanoTime();
        do {
            LoadTask task = tracker.current();
            if (task == null) {
                break;
            }
            try {
                if (!step(task)) {
                    // ワーカースレッドの完了待ち。描画を止めないよう次のフレームへ譲る。
                    break;
                }
            } catch (RuntimeException | LinkageError error) {
                fail(task, error);
                break;
            }
        } while (!tracker.finished() && System.nanoTime() - start < budgetNanos);

        if (tracker.finished()) {
            DiagnosticLog.info("Startup loading completed" + System.lineSeparator() + tracker.describe());
        }
        return tracker.finished();
    }

    /**
     * @return 処理を進めた場合true、ワーカーの完了待ちの場合false
     */
    private boolean step(LoadTask task) {
        if (task.state() == LoadTaskState.PENDING) {
            tracker.start(task);
        }
        if (task == discoveryTask) {
            // テーマ背景画像はMODに依存しないため、最初にワーカースレッドでデコードを始める。
            images = new ImagePreloader(UiFactory.BACKGROUND_IMAGES);
            images.start();
            // 探索途中で失敗してもMOD ClassLoaderを閉じられるよう、先にランタイムを受け取る。
            runtime = source.runtime();
            List<GameMod> mods = source.discoverMods();
            process = GameBootstrap.begin(mods, source.resources());
            tracker.detail(discoveryTask, mods.size() + " MOD(s): " + mods.stream().map(GameMod::id).toList());
            tracker.complete(discoveryTask);
            return true;
        }
        if (task == registerTask) {
            return stepPerMod(registerTask, BootstrapProcess.Stage.REGISTER_MODS, process::registerNextMod);
        }
        if (task == dataTask) {
            process.publishRegistrations();
            process.loadData();
            tracker.detail(dataTask, process.config().characters().size() + " characters, " + process.config().supports().size() + " supports");
            tracker.complete(dataTask);
            return true;
        }
        if (task == localizationTask) {
            process.loadLocalization();
            tracker.detail(localizationTask, process.config().game.language);
            tracker.complete(localizationTask);
            return true;
        }
        if (task == charactersTask) {
            process.registerCharacters();
            tracker.complete(charactersTask);
            return true;
        }
        if (task == abilitiesTask) {
            process.createAbilities();
            if (process.stage() == BootstrapProcess.Stage.COMPLETE) {
                startFontGeneration();
            }
            tracker.complete(abilitiesTask);
            return true;
        }
        if (task == initializeTask) {
            boolean progressed = stepPerMod(initializeTask, BootstrapProcess.Stage.INITIALIZE_MODS, process::initializeNextMod);
            if (task.state() == LoadTaskState.DONE) {
                startFontGeneration();
            }
            return progressed;
        }
        if (task == fontsTask) {
            return stepFonts();
        }
        if (task == uiTask) {
            if (!images.ready()) {
                tracker.detail(uiTask, "waiting for image decoder");
                return false;
            }
            Map<String, BitmapFont> prepared = new LinkedHashMap<>(fonts);
            // 所有権はSkinへ移る（組立てに失敗した場合もcreateSkinが解放する）。
            fonts.clear();
            skin = UiFactory.createSkin(prepared, images::texture);
            images.dispose();
            tracker.detail(uiTask, UiFactory.BACKGROUND_IMAGES.size() + " images");
            tracker.complete(uiTask);
            return true;
        }
        if (task == contextTask) {
            Skin prepared = skin;
            skin = null;
            context = new GameContext(snapshot, runtime, prepared);
            // 所有権はGameContextへ移った（イベント、MODランタイムを含む）。
            runtime = null;
            snapshot = null;
            process = null;
            tracker.complete(contextTask);
            return true;
        }
        if (task == screenTask) {
            firstScreen = firstScreenFactory.apply(context);
            tracker.detail(screenTask, firstScreen.getClass().getSimpleName());
            tracker.complete(screenTask);
            return true;
        }
        throw new IllegalStateException("Unknown startup task: " + task.debugName());
    }

    private boolean stepPerMod(LoadTask task, BootstrapProcess.Stage stage, Supplier<String> action) {
        int total = process.mods().size();
        if (process.stage() == stage) {
            String id = action.get();
            int done = process.stage() == stage ? process.modIndex() : total;
            if (total > 0) {
                tracker.units(task, done, total, id == null ? "" : id);
            }
        }
        if (process.stage() != stage) {
            tracker.complete(task);
        }
        return true;
    }

    /**
     * Skinに必要な文字（翻訳文、キャラクター、カスタムキャラクター名）はブートストラップ完了後に確定する。確定した時点でラスタライズを開始する。
     */
    private void startFontGeneration() {
        if (fontGenerator != null) {
            return;
        }
        snapshot = process.snapshot();
        fontGenerator = new AsyncFontGenerator(UiFactory.fontFile(), UiFactory.fontSpecs(snapshot.i18n(), GameContext.skinCharacters(snapshot)));
        fontGenerator.start();
    }

    private boolean stepFonts() {
        if (fontGenerator == null) {
            throw new IllegalStateException("Font generation was not started");
        }
        int total = fontGenerator.total();
        String next = fontGenerator.nextName();
        if (next == null) {
            finishFonts();
            return true;
        }
        tracker.units(fontsTask, fontGenerator.completed(), total, next);
        AsyncFontGenerator.NamedFont font = fontGenerator.poll();
        if (font == null) {
            return false;
        }
        fonts.put(font.name(), font.font());
        tracker.units(fontsTask, fontGenerator.completed(), total, font.name());
        if (fontGenerator.nextName() == null) {
            finishFonts();
        }
        return true;
    }

    private void finishFonts() {
        fontGenerator.dispose();
        tracker.detail(fontsTask, fonts.size() + " fonts");
        tracker.complete(fontsTask);
    }

    private void fail(LoadTask task, Throwable error) {
        failure = error;
        tracker.fail(task, error);
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
        if (images != null) {
            images.dispose();
        }
        DiagnosticLog.error("Startup loading failed at " + task.debugName() + System.lineSeparator() + tracker.describe(), error);
    }

    /**
     * 準備したゲームコンテキストと最初の画面を取り出す。以後の解放責任は受け取った側へ移る。
     */
    public Result takeResult() {
        if (!tracker.finished() || resultTaken || disposed) {
            throw new IllegalStateException("Startup loading result is not available");
        }
        resultTaken = true;
        Result result = new Result(context, firstScreen);
        context = null;
        firstScreen = null;
        return result;
    }

    /**
     * 受け渡していない要素とワーカーの成果物を解放する。複数回呼び出してもよい。
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        RuntimeException cleanup = new RuntimeException("Startup loader cleanup failed");
        if (fontGenerator != null) {
            closeStep(cleanup, fontGenerator::dispose);
        }
        if (images != null) {
            closeStep(cleanup, images::dispose);
        }
        if (firstScreen != null) {
            closeStep(cleanup, firstScreen::dispose);
            firstScreen = null;
        }
        if (context != null) {
            // コンテキストがSkin、効果音、イベント、MODランタイムを所有している。
            closeStep(cleanup, context::close);
            context = null;
        } else {
            for (BitmapFont font : fonts.values()) {
                closeStep(cleanup, font::dispose);
            }
            fonts.clear();
            if (skin != null) {
                closeStep(cleanup, skin::dispose);
                skin = null;
            }
            if (process != null) {
                closeStep(cleanup, process::abort);
                process = null;
            }
            if (runtime != null) {
                AutoCloseable closing = runtime;
                closeStep(cleanup, closing::close);
                runtime = null;
            }
        }
        if (cleanup.getSuppressed().length > 0) {
            DiagnosticLog.error("Startup loader cleanup failed", cleanup);
        }
    }

    private static void closeStep(RuntimeException cleanup, CloseStep step) {
        try {
            step.close();
        } catch (Exception | LinkageError error) {
            cleanup.addSuppressed(error);
        }
    }

    @FunctionalInterface
    private interface CloseStep {
        void close() throws Exception;
    }

    /**
     * 起動処理の成果物。
     *
     * @param context     ゲーム共有サービス。受け取った側が終了時に閉じる
     * @param firstScreen 最初に表示する画面
     */
    public record Result(GameContext context, Screen firstScreen) {
    }
}
