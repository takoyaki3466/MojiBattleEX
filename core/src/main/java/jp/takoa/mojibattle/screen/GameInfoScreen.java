package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.info.GameVersionInfo;
import jp.takoa.mojibattle.info.InstalledMod;
import jp.takoa.mojibattle.info.InstalledMods;

import java.util.List;

/**
 * ゲームのバージョン、APIバージョン、導入済みMODの一覧を表示する情報画面。
 *
 * <p>上段に2つのバージョンを大きな文字のカードで並べ、下段にMODを1件ずつカードで表示する。
 * MODが多い場合は下段だけがスクロールする。カードの背景には現在のテーマの {@code bar-bg} を使う。</p>
 */
public final class GameInfoScreen extends StageScreen {
    private static final float PANEL_WIDTH_RATIO = .72f;
    private static final float PANEL_HEIGHT_RATIO = .90f;
    private static final float CARD_GAP = 16f;
    private static final float SCROLLBAR_WIDTH = 8f;

    /** 見出し・補足の文字色。 */
    private static final Color CAPTION = new Color(.70f, .86f, .78f, 1f);
    /** 補足情報（ID、ファイル名）の文字色。 */
    private static final Color DETAIL = new Color(.80f, .86f, .82f, 1f);
    /** バージョン表記の強調色。 */
    private static final Color VERSION = new Color(1f, .84f, .38f, 1f);
    private static final Color EXTERNAL_TAG = new Color(.45f, .86f, 1f, 1f);
    private static final Color BUILT_IN_TAG = new Color(.62f, 1f, .66f, 1f);

    public GameInfoScreen(GameContext context, ScreenManager screens) {
        this(context, screens, GameVersionInfo.gameVersion(), GameVersionInfo.apiVersion(), InstalledMods.all());
    }

    GameInfoScreen(GameContext context, ScreenManager screens, String gameVersion, String apiVersion, List<InstalledMod> mods) {
        super(context, screens);

        Table root = new Table();

        Table panel = new Table(context.skin);
        panel.setBackground("panel");
        panel.pad(24, 32, 24, 32);
        panel.top();

        Label title = new Label(context.i18n.text("info.title"), context.skin, "title");
        title.setFontScale(.54f);
        title.setAlignment(Align.center);
        panel.add(title).height(64).growX().row();

        Table versions = new Table();
        versions.add(versionCard(context.i18n.text("info.game_version"), gameVersion)).grow().uniformX().padRight(CARD_GAP / 2f);
        versions.add(versionCard(context.i18n.text("info.api_version"), apiVersion)).grow().uniformX().padLeft(CARD_GAP / 2f);
        panel.add(versions).growX().padTop(8).row();

        Table modsHeader = new Table();
        Label modsTitle = new Label(context.i18n.text("info.mods"), context.skin);
        modsTitle.setColor(CAPTION);
        Label modsCount = new Label(context.i18n.text("info.mods.count", mods.size()), context.skin);
        modsCount.setColor(CAPTION);
        modsHeader.add(modsTitle).left().expandX();
        modsHeader.add(modsCount).right();
        panel.add(modsHeader).growX().padTop(26).padBottom(8).row();

        Table list = new Table();
        list.top();
        // スクロールバー表示時もカードと重ならないよう余白を空ける。
        list.padRight(12);
        if (mods.isEmpty()) {
            Table empty = card();
            Label none = new Label(context.i18n.text("info.mods.none"), context.skin);
            none.setColor(DETAIL);
            none.setAlignment(Align.center);
            empty.add(none).growX();
            list.add(empty).growX().row();
        }
        for (InstalledMod mod : mods) {
            list.add(modCard(mod)).growX().padBottom(10).row();
        }

        ScrollPane scroll = new ScrollPane(list, scrollStyle());
        // MODが多く一覧に収まらない場合だけ、右端にスクロールバーを表示する。
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        panel.add(scroll).grow().row();

        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });
        panel.add(back).width(360).height(56).padTop(16).center();

        addMenu(root, panel, PANEL_WIDTH_RATIO, PANEL_HEIGHT_RATIO);
        stage.setScrollFocus(scroll);
    }

    /** 見出しと大きな値を中央揃えで縦に並べたカード。 */
    private Table versionCard(String caption, String value) {
        Table card = card();
        Label captionLabel = new Label(caption, context.skin);
        captionLabel.setColor(CAPTION);
        captionLabel.setFontScale(.82f);
        captionLabel.setAlignment(Align.center);
        card.add(captionLabel).growX().row();

        Label valueLabel = new Label(value, context.skin, "title");
        valueLabel.setFontScale(.46f);
        valueLabel.setColor(VERSION);
        valueLabel.setAlignment(Align.center);
        card.add(valueLabel).growX().padTop(4);
        return card;
    }

    /**
     * 1件のMOD。1行目に名前とバージョン、2行目に読込み元・ID、外部MODでは3行目にJARファイル名を表示する。
     */
    private Table modCard(InstalledMod mod) {
        Table card = card();

        Label name = new Label(mod.name(), context.skin);
        name.setWrap(true);
        card.add(name).growX().left();
        if (!mod.version().isEmpty()) {
            Label version = new Label(context.i18n.text("info.mod.version", mod.version()), context.skin);
            version.setColor(VERSION);
            card.add(version).right().top().padLeft(20);
        }
        card.row();

        Table meta = new Table();
        boolean external = mod.source() == InstalledMod.Source.EXTERNAL;
        Label source = new Label(context.i18n.text(external ? "info.mod.external" : "info.mod.built_in"), context.skin);
        source.setFontScale(.82f);
        source.setColor(external ? EXTERNAL_TAG : BUILT_IN_TAG);
        Label id = new Label(context.i18n.text("info.mod.id", mod.id()), context.skin);
        id.setFontScale(.82f);
        id.setColor(DETAIL);
        meta.add(source).left().padRight(18);
        meta.add(id).left().expandX();
        card.add(meta).colspan(2).growX().left().padTop(4).row();

        if (external && !mod.file().isEmpty()) {
            Label file = new Label(context.i18n.text("info.mod.file", mod.file()), context.skin);
            file.setFontScale(.82f);
            file.setColor(DETAIL);
            file.setWrap(true);
            card.add(file).colspan(2).growX().left().padTop(2).row();
        }
        return card;
    }

    private ScrollPane.ScrollPaneStyle scrollStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        Drawable track = context.skin.newDrawable("bar-bg");
        track.setMinWidth(SCROLLBAR_WIDTH);
        Drawable knob = context.skin.newDrawable("bar-hp");
        knob.setMinWidth(SCROLLBAR_WIDTH);
        knob.setMinHeight(32f);
        style.vScroll = track;
        style.vScrollKnob = knob;
        return style;
    }

    private Table card() {
        Table card = new Table(context.skin);
        card.setBackground("bar-bg");
        card.pad(14, 20, 14, 20);
        return card;
    }
}
