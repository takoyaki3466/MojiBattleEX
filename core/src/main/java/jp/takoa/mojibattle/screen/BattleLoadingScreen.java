package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.crash.CrashReporter;
import jp.takoa.mojibattle.debug.DebugSettings;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.loading.LoadTask;
import jp.takoa.mojibattle.loading.LoadingDebugText;
import jp.takoa.mojibattle.loading.LoadTaskState;
import jp.takoa.mojibattle.loading.LoadingTracker;
import jp.takoa.mojibattle.log.DiagnosticLog;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * キャラクター選択完了から戦闘開始までの間に表示し、実際の準備処理と連動した進捗を示すロード画面。
 *
 * <p>準備処理は {@link BattleLoader} が1フレームあたり一定時間だけ進めるため、ロード中も描画が継続する。
 * 全タスクが成功するまで戦闘画面へは遷移せず、失敗した場合はクラッシュレポートを保存して
 * キャラクター選択へ戻る導線を表示する。</p>
 */
public final class BattleLoadingScreen extends StageScreen {
    /** 1フレームで準備処理に使う時間の目安。60FPSの1フレーム（約16.7ms）に描画の余裕を残す。 */
    private static final long WORK_BUDGET_NANOS = 10_000_000L;
    private static final float DEBUG_REFRESH_SECONDS = .1f;

    private final BattleMode mode;
    private final int playerCount;
    private final BattleLoader loader;
    private final LoadingTracker tracker;
    private final CrashReporter.LoadingInfo crashInfo;

    private final Label title;
    private final Label message;
    private final Label percent;
    private final ProgressBarWidget bar;
    private final Label debugDetails;
    private final TextButton back;

    private boolean firstFrameDrawn;
    private boolean readyToStart;
    private boolean handedOver;
    private boolean errorShown;
    private float debugRefreshTimer;

    public BattleLoadingScreen(GameContext context, ScreenManager screens, BattleMode mode, int playerCount, List<String> mainIds, List<String> supportIds, int supportCount) {
        super(context, screens);
        this.mode = mode;
        this.playerCount = playerCount;
        loader = new BattleLoader(context, mode, mainIds, supportIds, supportCount);
        tracker = loader.tracker();
        crashInfo = new CrashReporter.LoadingInfo() {
            @Override
            public String currentTask() {
                LoadTask task = tracker.current();
                return task == null ? "none" : task.debugName();
            }

            @Override
            public String describe() {
                return "Mode: " + mode + ", players=" + playerCount + ", mainIds=" + mainIds + ", supportIds=" + supportIds + ", supportCount=" + supportCount + System.lineSeparator() + tracker.describe();
            }
        };

        Table root = new Table();
        Table panel = new Table(context.skin);
        panel.setBackground("panel");
        panel.pad(28);
        panel.defaults().growX().pad(6);

        title = new Label(context.i18n.text("loading.title"), context.skin, "title");
        title.setFontScale(.42f);
        title.setAlignment(Align.center);
        panel.add(title).height(64).row();

        message = new Label("", context.skin);
        message.setAlignment(Align.center);
        message.setWrap(true);
        panel.add(message).minHeight(42).row();

        bar = new ProgressBarWidget(context.skin);
        panel.add(bar).height(26).padTop(10).row();

        percent = new Label("0%", context.skin);
        percent.setAlignment(Align.center);
        panel.add(percent).height(36).row();

        debugDetails = new Label("", context.skin);
        debugDetails.setFontScale(.5f);
        debugDetails.setAlignment(Align.topLeft);
        debugDetails.setWrap(true);
        panel.add(debugDetails).padTop(8).row();

        back = new TextButton(context.i18n.text("loading.error.back"), context.skin);
        back.setVisible(false);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                returnToSelection();
            }
        });
        panel.add(back).height(52).width(420).fill(false, false).expandX().center().padTop(12);

        addMenu(root, panel, .62f, .74f);
        refreshUi();
    }

    @Override
    public void show() {
        super.show();
        CrashReporter.setLoadingInfo(crashInfo);
        DiagnosticLog.info("Battle loading screen displayed; mode=" + mode);
    }

    @Override
    public void render(float delta) {
        if (handedOver) {
            return;
        }
        // 最初のフレームは処理を始めずに描画し、選択完了直後に必ずロード画面が見えるようにする。
        if (firstFrameDrawn && !errorShown) {
            if (readyToStart) {
                startBattle();
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
        refreshUi();
        super.render(delta);
        firstFrameDrawn = true;
    }

    private void startBattle() {
        PreparedBattle prepared = loader.takeResult();
        BattleScreen battle;
        try {
            battle = new BattleScreen(context, screens, prepared);
        } catch (RuntimeException | LinkageError error) {
            // BattleScreenのコンストラクタが準備済み要素を解放済み。
            showTransitionError(error);
            return;
        }
        try {
            handedOver = true;
            screens.show(battle);
        } catch (RuntimeException | LinkageError error) {
            // ScreenManagerが失敗した画面を破棄し、このロード画面へ戻している。
            handedOver = false;
            showTransitionError(error);
        }
    }

    private void showTransitionError(Throwable error) {
        DiagnosticLog.error("Battle screen transition failed after loading", error);
        Optional<Path> report = CrashReporter.report("Battle screen transition", "Battle screen could not be shown after loading completed", error);
        showError(context.i18n.text("loading.error.transition"), report);
    }

    private void showLoadingError(Throwable error) {
        LoadTask failed = tracker.failedTask();
        String taskName = failed == null ? "unknown" : failed.debugName();
        Optional<Path> report = CrashReporter.report("Battle loading: " + taskName, "Battle loading failed at task '" + taskName + "'", error);
        showError(failed == null ? taskName : taskMessage(failed), report);
    }

    private void showError(String failedTask, Optional<Path> report) {
        errorShown = true;
        title.setText(context.i18n.text("loading.error.title"));
        String location = report.map(Path::toString).orElse(null);
        message.setText(location == null ? context.i18n.text("loading.error.no_report", failedTask, String.valueOf(DiagnosticLog.file())) : context.i18n.text("loading.error.message", failedTask, location));
        back.setVisible(true);
        debugRefreshTimer = 0;
    }

    private void returnToSelection() {
        screens.show(new CharacterSelectScreen(context, screens, mode, playerCount));
    }

    private void refreshUi() {
        bar.progress = tracker.progress();
        percent.setText(tracker.percent() + "%");
        if (!errorShown) {
            LoadTask current = tracker.current();
            message.setText(current == null ? context.i18n.text("loading.complete") : taskMessage(current));
        }

        boolean debug = DebugSettings.enabled();
        debugDetails.setVisible(debug);
        if (debug && debugRefreshTimer <= 0) {
            debugRefreshTimer = DEBUG_REFRESH_SECONDS;
            debugDetails.setText(LoadingDebugText.format(tracker));
        } else if (!debug && debugDetails.getText().length() > 0) {
            debugDetails.setText("");
        }
    }

    private String taskMessage(LoadTask task) {
        if (task.totalUnits() > 0) {
            int shown = task.state() == LoadTaskState.DONE ? task.totalUnits() : Math.min(task.totalUnits(), task.completedUnits() + 1);
            return context.i18n.text(task.messageKey(), shown, task.totalUnits());
        }
        return context.i18n.text(task.messageKey(), 0, 0);
    }

    @Override
    public void dispose() {
        try {
            loader.dispose();
        } finally {
            CrashReporter.setLoadingInfo(null);
            super.dispose();
        }
    }

    /**
     * 既存SkinのバーDrawableを再利用する、表示専用の進捗バー。
     */
    private static final class ProgressBarWidget extends Widget {
        private final Drawable background;
        private final Drawable fill;
        private float progress;

        private ProgressBarWidget(Skin skin) {
            background = skin.getDrawable("bar-bg");
            fill = skin.getDrawable("bar-hp");
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
            background.draw(batch, getX(), getY(), getWidth(), getHeight());
            float width = getWidth() * Math.max(0f, Math.min(1f, progress));
            if (width > 0f) {
                fill.draw(batch, getX(), getY(), width, getHeight());
            }
        }

        @Override
        public float getPrefWidth() {
            return 400f;
        }

        @Override
        public float getPrefHeight() {
            return 26f;
        }
    }
}
