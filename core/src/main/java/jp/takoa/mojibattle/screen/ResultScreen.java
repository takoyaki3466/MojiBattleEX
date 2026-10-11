package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleResult;

public final class ResultScreen extends StageScreen {
    public ResultScreen(GameContext context, ScreenManager screens, BattleResult result) {
        super(context, screens); Table table = new Table(); table.setFillParent(true); table.defaults().width(400).height(64).pad(10);
        String text = result.draw() ? context.i18n.text("battle.draw") : context.i18n.text("battle.win",result.winner().label());
        table.add(new Label(text, context.skin)).row();
        TextButton back = new TextButton(context.i18n.text("menu.quit"), context.skin); table.add(back);
        back.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { screens.show(new TitleScreen(context, screens)); }});
        stage.addActor(table);
    }
}
