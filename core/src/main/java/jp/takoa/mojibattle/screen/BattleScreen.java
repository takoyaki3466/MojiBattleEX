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
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.TrainingDummy;
import jp.takoa.mojibattle.input.InputBindingManager;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.render.SplitScreenRenderer;
import jp.takoa.mojibattle.ui.BattleHud;

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
    private final InputBindingManager input;
    private final SplitScreenRenderer renderer;
    private final BattleHud hud;
    private final Table pauseMenu;
    private float accumulator;
    private boolean transitioned;
    private float resultCountdown = -1f;

    public BattleScreen(
            GameContext context,
            ScreenManager screens,
            BattleMode mode,
            List<String> mainIds,
            List<String> supportIds,
            int supportCount) {
        this.context = context;
        this.screens = screens;
        List<CharacterDefinition> characters = new ArrayList<>(
                mainIds.stream().map(context.characters::require).toList());
        List<List<SupportCharacterDefinition>> supports = new ArrayList<>();
        for (int player = 0; player < mainIds.size(); player++) {
            List<SupportCharacterDefinition> playerSupports = new ArrayList<>();
            for (int slot = 0; slot < supportCount; slot++) {
                int index = player * supportCount + slot;
                if (index < supportIds.size()) {
                    playerSupports.add(context.supports.require(supportIds.get(index)));
                }
            }
            supports.add(playerSupports);
        }
        if (mode == BattleMode.TRAINING) {
            characters.add(TrainingDummy.definition());
            supports.add(List.of());
        }
        battle = new BattleController(
                context.config.battle,
                context.config.critical,
                context.config.awakening,
                context.abilities,
                mode,
                characters,
                supports,
                context.config.fieldEffects());
        input = new InputBindingManager(context.config.controls);
        renderer = new SplitScreenRenderer(
                context.config.battle,
                context.config.graphics,
                characters,
                supports,
                context.config.skills(),
                context.config.ultimates(),
                context.config.awakening.texturePath);
        hud = new BattleHud(context, battle);
        pauseMenu = createPauseMenu();
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
                return;
            }
        }
    }

    private Table createPauseMenu() {
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
        hud.stage().addActor(table);
        return table;
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
        renderer.dispose();
        hud.dispose();
    }
}
