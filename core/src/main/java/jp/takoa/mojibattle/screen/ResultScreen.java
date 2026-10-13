package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleResult;

/** 確定した勝敗を表示し、タイトルへ戻る導線を提供する画面。 */
public final class ResultScreen extends StageScreen {
    public ResultScreen(GameContext context, ScreenManager screens, BattleResult result) {
        super(context, screens);
        Table root = new Table();
        Table table = new Table(context.skin);
        table.setBackground("panel");
        table.pad(28);
        table.defaults().width(520).height(72).pad(10);
        String text = result.draw()
                ? context.i18n.text("battle.draw")
                : context.i18n.text("battle.win", result.winner().label());
        table.add(new Label(text, context.skin)).row();
        TextButton back = new TextButton(context.i18n.text("menu.quit"), context.skin);
        table.add(back);
        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });
        addStandardMenu(root, table);
    }
}
