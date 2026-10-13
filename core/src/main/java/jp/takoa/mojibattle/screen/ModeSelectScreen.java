package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleMode;

/** 対戦人数またはトレーニングを選び、キャラクター選択画面へ遷移する画面。 */
public final class ModeSelectScreen extends StageScreen {
    public ModeSelectScreen(GameContext context, ScreenManager screens) {
        super(context, screens);
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);
        Table card = new Table(context.skin);
        card.setBackground("panel");
        card.pad(28);
        card.defaults().growX().height(56).pad(4);

        Label title = new Label(context.i18n.text("menu.player_count_title"), context.skin, "title");
        title.setFontScale(.62f);
        card.add(title).height(82).row();
        Label help = new Label(context.i18n.text("menu.player_count_help"), context.skin);
        help.setColor(.62f, 1f, .76f, 1f);
        card.add(help).height(45).row();

        addBattle(card, "menu.battle_two", 2);
        addBattle(card, "menu.battle_three", 3);
        addBattle(card, "menu.battle_four", 4);

        TextButton training = new TextButton(context.i18n.text("menu.training"), context.skin);
        card.add(training).row();
        training.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterSelectScreen(context, screens, BattleMode.TRAINING, 1));
            }
        });
        TextButton guide = new TextButton(context.i18n.text("menu.character_guide"), context.skin);
        card.add(guide).row();
        guide.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterGuideScreen(context, screens));
            }
        });
        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        card.add(back);
        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });
        addStandardMenu(root, card);
    }

    private void addBattle(Table table, String key, int players) {
        TextButton button = new TextButton(context.i18n.text(key), context.skin);
        table.add(button).row();
        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterSelectScreen(context, screens, BattleMode.VERSUS, players));
            }
        });
    }
}
