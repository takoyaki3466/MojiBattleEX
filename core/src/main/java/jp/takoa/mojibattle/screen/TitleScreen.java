package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;

/**
 * ゲーム開始、設定、キャラクター編集への入口を表示するタイトル画面。
 */
public final class TitleScreen extends StageScreen {
    public TitleScreen(GameContext context, ScreenManager screens) {
        this(context, screens, null);
    }

    /**
     * @param noticeKey 保存完了など、一時的に表示する翻訳キー。不要な場合は {@code null}
     */
    public TitleScreen(GameContext context, ScreenManager screens, String noticeKey) {
        super(context, screens);
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        Table hero = new Table(context.skin);
        hero.setBackground("panel");
        hero.pad(32);
        hero.defaults().growX().height(72).pad(7);

        Label title = new Label(context.i18n.text("menu.title"), context.skin, "title");
        title.setAlignment(Align.center);
        hero.add(title).height(120).row();

        Label subtitle = new Label(context.i18n.text("menu.subtitle"), context.skin);
        subtitle.setColor(.62f, 1f, .76f, 1f);
        subtitle.setAlignment(Align.center);
        hero.add(subtitle).height(54).row();

        TextButton start = new TextButton(context.i18n.text("menu.start"), context.skin);
        hero.add(start).height(82).row();

        TextButton editor = new TextButton(context.i18n.text("menu.character_editor"), context.skin);
        hero.add(editor).row();

        TextButton settings = new TextButton(context.i18n.text("menu.settings"), context.skin);
        hero.add(settings).row();

        if (noticeKey != null) {
            Label saved = new Label(context.i18n.text(noticeKey), context.skin);

            saved.setColor(.65f, 1f, .72f, 1f);
            saved.setAlignment(Align.center);

            hero.add(saved).height(44).row();
        }

        start.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new ModeSelectScreen(context, screens));
            }
        });

        settings.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new SettingsScreen(context, screens));
            }
        });

        editor.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new CharacterEditorScreen(context, screens));
            }
        });

        addStandardMenu(root, hero);

        Label aiNotice = new Label(context.i18n.text("menu.ai_notice"), context.skin);

        aiNotice.setFontScale(.46f);
        aiNotice.setWrap(true);
        aiNotice.setAlignment(Align.right);

        Table notice = new Table();
        notice.setFillParent(true);
        notice.bottom().right().pad(10);
        notice.add(aiNotice).width(610).right();

        stage.addActor(notice);
    }
}
