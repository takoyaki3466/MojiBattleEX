package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.domain.BattleTeamDefinition;
import jp.takoa.mojibattle.input.PlayerInputManager;
import jp.takoa.mojibattle.loading.LoadTask;
import jp.takoa.mojibattle.loading.LoadTaskState;
import jp.takoa.mojibattle.loading.LoadingTracker;
import jp.takoa.mojibattle.log.DiagnosticLog;
import jp.takoa.mojibattle.render.ArenaRenderer;
import jp.takoa.mojibattle.render.BattleTextureCache;
import jp.takoa.mojibattle.render.SplitScreenRenderer;
import jp.takoa.mojibattle.resource.GdxTextureLoader;
import jp.takoa.mojibattle.ui.BattleHud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * キャラクター選択完了後の戦闘準備を、フレームごとに少しずつ進める。
 *
 * <p>処理は次のタスクに分かれ、状態は {@link LoadingTracker} へ記録する。</p>
 * <ol>
 *     <li>キャラクターデータの確認（編成の検証、使用画像の一覧化）</li>
 *     <li>能力と戦闘状態の初期化（{@link BattleController}。MODの能力生成を含むためGLスレッドで実行）</li>
 *     <li>テクスチャの読込み（読出しとデコードはワーカースレッド、GPU転送はGLスレッドで1件ずつ）</li>
 *     <li>ステージの3Dモデル構築（先読みしたTextureを使用）</li>
 *     <li>入力と戦闘UIの準備</li>
 * </ol>
 * <p>OpenGL・ゲーム状態・MODコードはGLスレッドでのみ扱い、ワーカースレッドはリソースの読出しと
 * 画像デコード（OpenGLを使わない処理）だけを行う。</p>
 */
final class BattleLoader implements Disposable {
    private static final int MAX_DECODES_IN_FLIGHT = 3;
    private static final String DECODER_THREAD_NAME = "MojiBattle-TextureDecoder";

    private final GameContext context;
    private final BattleMode mode;
    private final List<String> mainIds;
    private final List<String> supportIds;
    private final int supportCount;

    private final LoadingTracker tracker;
    private final LoadTask teamsTask;
    private final LoadTask battleTask;
    private final LoadTask texturesTask;
    private final LoadTask arenaTask;
    private final LoadTask hudTask;

    private final GdxTextureLoader decodeLoader;
    private final Deque<PendingDecode> inFlight = new ArrayDeque<>();
    private ExecutorService decoder;

    private List<BattleTeamDefinition> teams;
    private List<String> textureReferences = List.of();
    private int nextSubmission;
    private int uploaded;
    private long slowestUploadNanos;
    private String slowestUpload = "";

    private BattleController battle;
    private PlayerInputManager input;
    private BattleTextureCache textures;
    private SplitScreenRenderer renderer;
    private BattleHud hud;

    private Throwable failure;
    private boolean resultTaken;
    private boolean disposed;

    BattleLoader(GameContext context, BattleMode mode, List<String> mainIds, List<String> supportIds, int supportCount) {
        this(context, mode, mainIds, supportIds, supportCount, new LoadingTracker());
    }

    BattleLoader(GameContext context, BattleMode mode, List<String> mainIds, List<String> supportIds, int supportCount, LoadingTracker tracker) {
        this.context = context;
        this.mode = mode;
        this.mainIds = List.copyOf(mainIds);
        this.supportIds = List.copyOf(supportIds);
        this.supportCount = supportCount;
        this.tracker = tracker;
        decodeLoader = new GdxTextureLoader(context.resources);

        teamsTask = tracker.add("Character Data", "loading.task.characters", 1f);
        battleTask = tracker.add("Abilities & Battle State", "loading.task.battle_state", 2f);
        texturesTask = tracker.add("Texture Loading", "loading.task.textures", 14f);
        arenaTask = tracker.add("Stage Models", "loading.task.stage", 2f);
        hudTask = tracker.add("Battle UI", "loading.task.hud", 1f);
    }

    LoadingTracker tracker() {
        return tracker;
    }

    Throwable failure() {
        return failure;
    }

    boolean finished() {
        return tracker.finished();
    }

    /**
     * 指定時間を目安に準備を進める。1件の処理は途中で中断しないため、目安を超える場合がある。
     *
     * @return 全タスクが成功していればtrue
     */
    boolean update(long budgetNanos) {
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
            DiagnosticLog.info("Battle loading completed" + System.lineSeparator() + tracker.describe());
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
        if (task == teamsTask) {
            teams = BattleScreen.buildTeams(context, mode, mainIds, supportIds, supportCount);
            textureReferences = ArenaRenderer.requiredTextures(teams, context.config.skills(), context.config.ultimates(), context.config.awakening.texturePath, context.registries.effects.values());
            tracker.detail(teamsTask, mainIds.size() + " main / " + supportIds.size() + " support, " + textureReferences.size() + " textures");
            startDecoder();
            tracker.complete(teamsTask);
            return true;
        }
        if (task == battleTask) {
            // 先読みはこの間もワーカースレッドで進む。
            tracker.detail(battleTask, mode.name());
            battle = BattleScreen.createBattleController(context, mode, teams);
            tracker.complete(battleTask);
            return true;
        }
        if (task == texturesTask) {
            return stepTextures();
        }
        if (task == arenaTask) {
            BattleTextureCache prepared = textures;
            renderer = new SplitScreenRenderer(context.config.battle, context.config.graphics, teams, context.config.skills(), context.config.ultimates(), context.config.awakening.texturePath, context.registries.effects.values(), prepared);
            // 所有権はRendererへ移った。
            textures = null;
            tracker.complete(arenaTask);
            return true;
        }
        if (task == hudTask) {
            input = BattleScreen.createInput(context);
            hud = new BattleHud(context, battle);
            tracker.complete(hudTask);
            return true;
        }
        throw new IllegalStateException("Unknown load task: " + task.debugName());
    }

    private boolean stepTextures() {
        if (textures == null) {
            textures = new BattleTextureCache(context.resources);
        }
        int total = textureReferences.size();
        if (uploaded >= total) {
            finishTextures();
            return true;
        }
        submitDecodes();
        PendingDecode head = inFlight.peekFirst();
        if (head == null) {
            throw new IllegalStateException("Texture decode queue is empty before all textures were loaded");
        }
        tracker.detail(texturesTask, head.reference);
        Pixmap pixmap = head.take();
        if (pixmap == null) {
            tracker.units(texturesTask, uploaded, total, head.reference);
            return false;
        }
        inFlight.removeFirst();
        String size = pixmap.getWidth() + "x" + pixmap.getHeight();
        long uploadStart = System.nanoTime();
        try {
            textures.upload(head.reference, pixmap);
        } finally {
            pixmap.dispose();
        }
        long uploadNanos = System.nanoTime() - uploadStart;
        if (uploadNanos > slowestUploadNanos) {
            slowestUploadNanos = uploadNanos;
            slowestUpload = head.reference + " " + size;
        }
        uploaded++;
        tracker.units(texturesTask, uploaded, total, head.reference);
        submitDecodes();
        if (uploaded >= total) {
            finishTextures();
        }
        return true;
    }

    private void finishTextures() {
        stopDecoder();
        if (!slowestUpload.isEmpty()) {
            // GPU転送で最も時間のかかった画像。フリーズ要因の特定に使う。
            tracker.detail(texturesTask, "slowest upload: " + slowestUpload + " (" + LoadingTracker.formatMillis(slowestUploadNanos) + ")");
        }
        tracker.complete(texturesTask);
    }

    private void startDecoder() {
        if (decoder != null || textureReferences.isEmpty()) {
            return;
        }
        decoder = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, DECODER_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        submitDecodes();
    }

    private void submitDecodes() {
        if (decoder == null) {
            return;
        }
        while (inFlight.size() < MAX_DECODES_IN_FLIGHT && nextSubmission < textureReferences.size()) {
            PendingDecode pending = new PendingDecode(textureReferences.get(nextSubmission));
            try {
                decoder.execute(() -> pending.run(decodeLoader));
            } catch (RejectedExecutionException rejected) {
                throw new IllegalStateException("Texture decoder is not available", rejected);
            }
            inFlight.addLast(pending);
            nextSubmission++;
        }
    }

    private void stopDecoder() {
        while (!inFlight.isEmpty()) {
            inFlight.removeFirst().abandon();
        }
        if (decoder != null) {
            decoder.shutdownNow();
            decoder = null;
        }
    }

    private void fail(LoadTask task, Throwable error) {
        failure = error;
        tracker.fail(task, error);
        stopDecoder();
        DiagnosticLog.error("Battle loading failed at " + task.debugName() + System.lineSeparator() + tracker.describe(), error);
    }

    /**
     * 準備した戦闘要素を取り出す。以後の解放責任は受け取った側へ移る。
     */
    PreparedBattle takeResult() {
        if (!tracker.finished() || resultTaken || disposed) {
            throw new IllegalStateException("Battle loading result is not available");
        }
        resultTaken = true;
        PreparedBattle prepared = new PreparedBattle(battle, input, renderer, hud);
        battle = null;
        input = null;
        renderer = null;
        hud = null;
        return prepared;
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
        stopDecoder();
        RuntimeException cleanupFailure = new RuntimeException("Battle loader cleanup failed");
        new PreparedBattle(battle, input, renderer, hud).disposeAfterFailure(cleanupFailure);
        battle = null;
        input = null;
        renderer = null;
        hud = null;
        if (textures != null) {
            BattleScreen.closeAfterFailure(cleanupFailure, textures::dispose);
            textures = null;
        }
        if (cleanupFailure.getSuppressed().length > 0) {
            DiagnosticLog.error("Battle loader cleanup failed", cleanupFailure);
        }
    }

    /**
     * ワーカースレッドで1件の画像を読み出してデコードし、GLスレッドへ受け渡す。
     * 受け渡し前に破棄された場合は、ワーカー側でPixmapを解放する。
     */
    private static final class PendingDecode {
        private final String reference;
        private Pixmap pixmap;
        private Throwable error;
        private String errorThread;
        private boolean done;
        private boolean abandoned;

        private PendingDecode(String reference) {
            this.reference = reference;
        }

        private void run(GdxTextureLoader loader) {
            Pixmap decoded = null;
            Throwable decodeFailure = null;
            try {
                synchronized (this) {
                    if (abandoned) {
                        return;
                    }
                }
                decoded = loader.decode(reference);
            } catch (Throwable thrown) {
                decodeFailure = thrown;
            }
            synchronized (this) {
                if (abandoned) {
                    if (decoded != null) {
                        decoded.dispose();
                    }
                    return;
                }
                pixmap = decoded;
                error = decodeFailure;
                errorThread = Thread.currentThread().getName();
                done = true;
            }
        }

        /**
         * デコード済みPixmapを受け取る。未完了ならnull。デコード失敗時は例外を送出する。
         */
        private synchronized Pixmap take() {
            if (!done) {
                return null;
            }
            if (error != null) {
                if (error instanceof VirtualMachineError fatal) {
                    throw fatal;
                }
                throw new IllegalStateException("Cannot decode texture '" + reference + "' on thread " + errorThread, error);
            }
            Pixmap taken = pixmap;
            pixmap = null;
            return taken;
        }

        private synchronized void abandon() {
            abandoned = true;
            if (pixmap != null) {
                pixmap.dispose();
                pixmap = null;
            }
        }
    }
}
