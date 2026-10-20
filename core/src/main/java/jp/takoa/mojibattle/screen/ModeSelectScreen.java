package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.ui.ChaosButton;
import jp.takoa.mojibattle.ui.ChaosMenuLayout;
import jp.takoa.mojibattle.ui.UiTheme;
import jp.takoa.mojibattle.ui.UiThemeManager;

/**
 * 対戦人数またはトレーニングを選び、キャラクター選択画面へ遷移する画面。
 */
public final class ModeSelectScreen extends StageScreen {
    public ModeSelectScreen(GameContext context, ScreenManager screens) {
        super(context, screens);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        Label title = new Label(context.i18n.text("menu.player_count_title"), context.skin, "title");
        title.setFontScale(.62f);
        Label help = new Label(context.i18n.text("menu.player_count_help"), context.skin);
        help.setColor(.62f, 1f, .76f, 1f);

        boolean chaosUi = UiThemeManager.selected() == UiTheme.CHAOS;
        TextButton training = chaosUi
                ? new ChaosButton(context.i18n.text("menu.training"), context.skin, ChaosButton.Accent.YELLOW)
                : new TextButton(context.i18n.text("menu.training"), context.skin);
        TextButton guide = chaosUi
                ? new ChaosButton(context.i18n.text("menu.character_guide"), context.skin, ChaosButton.Accent.CYAN)
                : new TextButton(context.i18n.text("menu.character_guide"), context.skin);
        TextButton back = chaosUi
                ? new ChaosButton(context.i18n.text("menu.back"), context.skin, ChaosButton.Accent.STEEL)
                : new TextButton(context.i18n.text("menu.back"), context.skin);

        if (chaosUi) {
            ChaosMenuLayout layout = new ChaosMenuLayout(context.skin).header(title, help);
            addChaosBattleRow(layout, "menu.battle_two", "menu.battle_chaos_two", 2, ChaosButton.Accent.CYAN);
            addChaosBattleRow(layout, "menu.battle_three", "menu.battle_chaos_three", 3, ChaosButton.Accent.LIME);
            addChaosBattleRow(layout, "menu.battle_four", "menu.battle_chaos_four", 4, ChaosButton.Accent.MAGENTA);
            layout.action(training, 64).action(guide, 60).action(back, 58).finish();
            addMenu(root, layout.table(), .96f, .96f);
        } else {
            Table card = new Table(context.skin);
            card.setBackground("panel");
            card.pad(28);
            card.defaults().growX().height(56).pad(4);

            card.add(title).height(82).row();
            card.add(help).height(45).row();

            addBattleRow(card, "menu.battle_two", "menu.battle_chaos_two", 2);
            addBattleRow(card, "menu.battle_three", "menu.battle_chaos_three", 3);
            addBattleRow(card, "menu.battle_four", "menu.battle_chaos_four", 4);
            card.add(training).row();
            card.add(guide).row();
            card.add(back);
            addStandardMenu(root, card);
        }

        training.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterSelectScreen(context, screens, BattleMode.TRAINING, 1));
            }
        });

        guide.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterGuideScreen(context, screens));
            }
        });

        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });

    }

    private void addChaosBattleRow(ChaosMenuLayout layout, String versusKey, String chaosKey,
                                   int players, ChaosButton.Accent accent) {
        TextButton versus = new ChaosButton(context.i18n.text(versusKey), context.skin, accent);
        TextButton chaos = new ChaosButton(
                context.i18n.text(chaosKey), context.skin, ChaosButton.Accent.ORANGE);
        layout.pair(versus, chaos, 66);
        addBattleListeners(versus, chaos, players);
    }

    /** 通常対戦と同人数のカオス対戦を1行に並べ、人数ごとの入口を揃える。 */
    private void addBattleRow(Table table, String versusKey, String chaosKey, int players) {
        Table row = new Table();
        row.defaults().growX().height(52).pad(2);

        TextButton versus = new TextButton(context.i18n.text(versusKey), context.skin);
        TextButton chaos = new TextButton(context.i18n.text(chaosKey), context.skin);
        row.add(versus);
        row.add(chaos);
        table.add(row).growX().height(60).row();

        addBattleListeners(versus, chaos, players);
    }

    private void addBattleListeners(TextButton versus, TextButton chaos, int players) {
        versus.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterSelectScreen(context, screens, BattleMode.VERSUS, players));
            }
        });
        chaos.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterSelectScreen(context, screens, BattleMode.CHAOS, players));
            }
        });
    }
}
