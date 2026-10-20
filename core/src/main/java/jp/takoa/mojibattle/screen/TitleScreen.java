package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.ui.ChaosButton;
import jp.takoa.mojibattle.ui.ChaosMenuLayout;
import jp.takoa.mojibattle.ui.UiTheme;
import jp.takoa.mojibattle.ui.UiThemeManager;

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

        Label title = new Label(context.i18n.text("menu.title"), context.skin, "title");
        title.setAlignment(Align.center);

        Label subtitle = new Label(context.i18n.text("menu.subtitle"), context.skin);
        subtitle.setColor(.62f, 1f, .76f, 1f);
        subtitle.setAlignment(Align.center);

        boolean chaos = UiThemeManager.selected() == UiTheme.CHAOS;
        TextButton start = chaos
                ? new ChaosButton(context.i18n.text("menu.start"), context.skin, ChaosButton.Accent.ORANGE)
                : new TextButton(context.i18n.text("menu.start"), context.skin);
        TextButton editor = chaos
                ? new ChaosButton(context.i18n.text("menu.character_editor"), context.skin, ChaosButton.Accent.CYAN)
                : new TextButton(context.i18n.text("menu.character_editor"), context.skin);
        TextButton settings = chaos
                ? new ChaosButton(context.i18n.text("menu.settings"), context.skin, ChaosButton.Accent.LIME)
                : new TextButton(context.i18n.text("menu.settings"), context.skin);

        Label saved = noticeKey == null ? null : new Label(context.i18n.text(noticeKey), context.skin);
        if (saved != null) {
            saved.setColor(.65f, 1f, .72f, 1f);
            saved.setAlignment(Align.center);
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

        if (chaos) {
            title.setFontScale(1.08f);
            ChaosMenuLayout layout = new ChaosMenuLayout(context.skin)
                    .header(title, subtitle)
                    .action(start, 84)
                    .action(editor, 76)
                    .action(settings, 76);
            if (saved != null) {
                layout.notice(saved);
            }
            layout.finish();
            addMenu(root, layout.table(), .94f, .93f);
        } else {
            Table hero = new Table(context.skin);
            hero.setBackground("panel");
            hero.pad(32);
            hero.defaults().growX().height(72).pad(7);
            hero.add(title).height(120).row();
            hero.add(subtitle).height(54).row();
            hero.add(start).height(82).row();
            hero.add(editor).row();
            hero.add(settings).row();
            if (saved != null) {
                hero.add(saved).height(44).row();
            }
            addStandardMenu(root, hero);
        }

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
