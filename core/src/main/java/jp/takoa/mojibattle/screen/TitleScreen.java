package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;

public final class TitleScreen extends StageScreen {
    public TitleScreen(GameContext context,ScreenManager screens){
        super(context,screens);
        Table root=new Table();root.setFillParent(true);root.top().left().pad(46);
        Table hero=new Table(context.skin);hero.setBackground("panel");hero.pad(28);hero.defaults().left();
        Label title=new Label(context.i18n.text("menu.title"),context.skin,"title");hero.add(title).width(760).height(118).left().row();
        Label subtitle=new Label(context.i18n.text("menu.subtitle"),context.skin);subtitle.setColor(.62f,1f,.76f,1f);hero.add(subtitle).padBottom(28).left().row();
        TextButton start=new TextButton(context.i18n.text("menu.start"),context.skin);hero.add(start).width(430).height(78).padBottom(12).left().row();
        TextButton settings=new TextButton(context.i18n.text("menu.settings"),context.skin);hero.add(settings).width(430).height(68).left();
        start.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){screens.show(new ModeSelectScreen(context,screens));}});
        settings.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){screens.show(new SettingsScreen(context,screens));}});
        root.add(hero).top().left();stage.addActor(root);
        Label aiNotice=new Label(context.i18n.text("menu.ai_notice"),context.skin);aiNotice.setFontScale(.46f);aiNotice.setWrap(true);aiNotice.setAlignment(Align.right);
        Table notice=new Table();notice.setFillParent(true);notice.bottom().right().pad(10);notice.add(aiNotice).width(610).right();stage.addActor(notice);
    }
}
