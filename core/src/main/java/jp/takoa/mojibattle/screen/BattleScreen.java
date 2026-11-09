package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.domain.BattleModeRoster;
import jp.takoa.mojibattle.domain.BattleTeamDefinition;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.TrainingDummy;
import jp.takoa.mojibattle.input.InputDeviceMode;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.input.PlayerInputManager;
import jp.takoa.mojibattle.render.SplitScreenRenderer;
import jp.takoa.mojibattle.ui.BattleHud;
import jp.takoa.mojibattle.ui.UiThemeManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * 入力収集、固定時間ステップの戦闘更新、分割画面描画およびHUD描画を調停する戦闘画面。
 */
public final class BattleScreen extends ScreenAdapter {
    private static final float MAX_FRAME_DELTA = 0.25f;
    private final GameContext context;
    private final ScreenManager screens;
    private final BattleController battle;
    private final PlayerInputManager input;
    private final SplitScreenRenderer renderer;
    private final BattleHud hud;
    private final Table pauseMenu;
    private float accumulator;
    private boolean transitioned;
    private float resultCountdown = -1f;

    /**
     * 戦闘に必要な全要素をこの場で同期的に構築する。ロード画面を経由しない従来の生成経路。
     */
    public BattleScreen(GameContext context, ScreenManager screens, BattleMode mode, List<String> mainIds, List<String> supportIds, int supportCount) {
        this(context, screens, prepareSynchronously(context, mode, mainIds, supportIds, supportCount));
    }

    /**
     * {@link BattleLoadingScreen} が準備した戦闘要素から画面を構築する。
     * 構築に成功した時点で各要素の所有権はこの画面へ移る。失敗した場合は各要素を解放する。
     */
    BattleScreen(GameContext context, ScreenManager screens, PreparedBattle prepared) {
        this.context = context;
        this.screens = screens;

        Table createdPauseMenu;
        try {
            createdPauseMenu = createPauseMenu(prepared.hud());
        } catch (RuntimeException | Error error) {
            prepared.disposeAfterFailure(error);
            throw error;
        }

        battle = prepared.battle();
        input = prepared.input();
        renderer = prepared.renderer();
        hud = prepared.hud();
        pauseMenu = createdPauseMenu;
    }

    private static PreparedBattle prepareSynchronously(GameContext context, BattleMode mode, List<String> mainIds, List<String> supportIds, int supportCount) {
        BattleController createdBattle = null;
        PlayerInputManager createdInput = null;
        SplitScreenRenderer createdRenderer = null;
        BattleHud createdHud = null;

        try {
            List<BattleTeamDefinition> teams = buildTeams(context, mode, mainIds, supportIds, supportCount);

            createdBattle = createBattleController(context, mode, teams);
            createdInput = createInput(context);
            createdRenderer = new SplitScreenRenderer(context.config.battle, context.config.graphics, teams, context.config.skills(), context.config.ultimates(), context.config.awakening.texturePath, context.registries.effects.values(), context.resources);
            createdHud = new BattleHud(context, createdBattle);
            return new PreparedBattle(createdBattle, createdInput, createdRenderer, createdHud);

        } catch (RuntimeException | Error error) {
            new PreparedBattle(createdBattle, createdInput, createdRenderer, createdHud).disposeAfterFailure(error);
            throw error;
        }
    }

    /**
     * 戦闘状態、能力ロードアウト、補助キャラクターを初期化する。
     */
    static BattleController createBattleController(GameContext context, BattleMode mode, List<BattleTeamDefinition> teams) {
        BattleController createdBattle = new BattleController(context.config.battle, context.config.critical, context.config.awakening, context.abilities, mode, teams, context.config.fieldEffects(), context.registries.supportAbilityImplementations, context.registries.supportPassiveImplementations, context.events.gameplay());
        try {
            context.config.graphics.ultimateCutIns = Gdx.app.getPreferences(UiThemeManager.PREFERENCES_NAME).getBoolean("ultimateCutIns", context.config.graphics.ultimateCutIns);
            createdBattle.setCutInsEnabled(context.config.graphics.ultimateCutIns);
            return createdBattle;
        } catch (RuntimeException | Error error) {
            closeAfterFailure(error, createdBattle::close);
            throw error;
        }
    }

    static PlayerInputManager createInput(GameContext context) {
        InputDeviceMode inputMode = InputDeviceMode.load(Gdx.app.getPreferences(UiThemeManager.PREFERENCES_NAME));
        return new PlayerInputManager(context.config.controls, context.joyCons, inputMode);
    }

    static List<BattleTeamDefinition> buildTeams(GameContext context, BattleMode mode, List<String> mainIds, List<String> supportIds, int supportCount) {
        int mainCount = BattleModeRoster.mainCount(mode);

        if (mode == BattleMode.MEGA_GALAXY && (mainIds.size() != 2 * mainCount || supportCount != BattleModeRoster.supportCount(mode) || supportIds.size() != 2 * supportCount)) {
            throw new IllegalArgumentException("Mega Galaxy mode requires 2 complete teams");
        }

        int playerCount = mode == BattleMode.TRAINING ? 1 : mainIds.size() / mainCount;

        List<BattleTeamDefinition> teams = new ArrayList<>();

        for (int player = 0; player < playerCount; player++) {
            int mainOffset = player * mainCount;

            CharacterDefinition representative = context.characters.require(mainIds.get(mainOffset));
            List<CharacterDefinition> subMains = new ArrayList<>();

            for (int slot = 1; slot < mainCount; slot++) {
                subMains.add(context.characters.require(mainIds.get(mainOffset + slot)));
            }

            List<SupportCharacterDefinition> playerSupports = new ArrayList<>();

            for (int slot = 0; slot < supportCount; slot++) {
                int index = player * supportCount + slot;

                if (index < supportIds.size()) {
                    playerSupports.add(context.supports.require(supportIds.get(index)));
                }
            }

            teams.add(new BattleTeamDefinition(representative, subMains, playerSupports));
        }

        if (mode == BattleMode.TRAINING) {
            teams.add(new BattleTeamDefinition(TrainingDummy.definition(), List.of(), List.of()));
        }
        return List.copyOf(teams);
    }

    @Override
    public void show() {
        InputAdapter trainingKeys = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (battle.mode() != BattleMode.TRAINING) {
                    return false;
                }

                switch (keycode) {
                    case Input.Keys.F1 -> battle.trainingResetCooldown();
                    case Input.Keys.F2 -> battle.trainingMaxUltimate();
                    case Input.Keys.F3 -> battle.trainingResetStatistics();
                    case Input.Keys.F4 -> battle.trainingSetElapsed(battle.timer().elapsed() + 30);
                    case Input.Keys.F5 -> battle.trainingSpawnAwakening();

                    default -> {
                        return false;
                    }
                }
                return true;
            }
        };
        Gdx.input.setInputProcessor(new InputMultiplexer(hud.stage(), trainingKeys, input));
    }

    @Override
    public void render(float delta) {
        if (input.consumePause()) {
            togglePause();
        }

        float frameDelta = Math.min(MAX_FRAME_DELTA, Math.max(0, delta));

        if (!battle.paused()) {
            accumulator += frameDelta;

            while (accumulator >= context.config.battle.fixedStepSeconds) {
                input.refreshControllers();
                EnumMap<PlayerId, PlayerCommand> commands = new EnumMap<>(PlayerId.class);

                for (PlayerId id : battle.playerIds()) {
                    commands.put(id, input.consume(id));
                }

                battle.update(context.config.battle.fixedStepSeconds, commands);
                accumulator -= context.config.battle.fixedStepSeconds;

                if (battle.result() != null) {
                    break;
                }
            }
        }
        context.audio.play(battle.drainSoundEvents());

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        renderer.render(battle, frameDelta, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        hud.draw(battle);

        if (battle.result() != null && !transitioned) {
            if (resultCountdown < 0) {
                resultCountdown = 1f;
            } else {
                resultCountdown -= frameDelta;
            }

            if (resultCountdown <= 0) {
                transitioned = true;
                screens.show(new ResultScreen(context, screens, battle.result()));
            }
        }
    }

    private Table createPauseMenu(BattleHud targetHud) {
        Table table = new Table(context.skin);
        table.setBackground("panel");
        table.setFillParent(true);
        table.setVisible(false);
        table.defaults().width(320).height(58).pad(8);

        TextButton resume = new TextButton(context.i18n.text("menu.resume"), context.skin);
        table.add(resume).row();

        TextButton quit = new TextButton(context.i18n.text("menu.quit"), context.skin);
        table.add(quit);

        resume.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });
        quit.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });

        targetHud.stage().addActor(table);
        return table;
    }

    static void closeAfterFailure(Throwable failure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error cleanupError) {
            failure.addSuppressed(cleanupError);
        }
    }

    private void togglePause() {
        battle.setPaused(!battle.paused());
        input.clear();
        pauseMenu.setVisible(battle.paused());
    }

    @Override
    public void resize(int width, int height) {
        hud.resize(width, height);
    }

    @Override
    public void pause() {
        input.clear();
        battle.setPaused(true);
        pauseMenu.setVisible(true);
    }

    @Override
    public void dispose() {
        input.clear();
        battle.close();
        renderer.dispose();
        hud.dispose();
    }
}
