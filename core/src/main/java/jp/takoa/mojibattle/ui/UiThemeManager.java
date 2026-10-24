package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * UIテーマを保存し、共通SkinのDrawableと文字色だけを差し替える。
 * 画面のTableやCellには触れないため、テーマ変更で操作位置は変化しない。
 */
public final class UiThemeManager {
    public static final String PREFERENCES_NAME = "mojibattle-user";
    private static final String PREFERENCE_KEY = "uiTheme";

    private UiThemeManager() {
    }

    /**
     * 保存済みのテーマを返す。
     */
    public static UiTheme selected() {
        return UiTheme.fromId(preferences().getInteger(PREFERENCE_KEY, UiTheme.CURRENT.id()));
    }

    /**
     * 選択を保存し、Skinへ即時反映する。
     */
    public static void select(Skin skin, UiTheme theme) {
        preferences().putInteger(PREFERENCE_KEY, theme.id()).flush();
        apply(skin, theme);
    }

    /**
     * 配置を変更せず、Skinが解決する見た目だけをテーマへ置き換える。
     */
    public static void apply(Skin skin, UiTheme theme) {
        installDrawables(skin, theme);

        Color textColor = switch (theme) {
            case CURRENT, MODERN -> Color.WHITE;
            case CHAOS -> new Color(1f, 1f, .72f, 1f);
        };

        BitmapFont bodyFont = switch (theme) {
            case CURRENT -> skin.get("default-font", BitmapFont.class);
            case MODERN -> skin.get("modern-font", BitmapFont.class);
            case CHAOS -> skin.get("chaos-font", BitmapFont.class);
        };
        BitmapFont titleFont = switch (theme) {
            case CURRENT -> skin.get("default-title-font", BitmapFont.class);
            case MODERN -> skin.get("modern-title-font", BitmapFont.class);
            case CHAOS -> skin.get("chaos-title-font", BitmapFont.class);
        };
        BitmapFont effectFont = switch (theme) {
            case CURRENT -> skin.get("default-effect-font", BitmapFont.class);
            case MODERN -> skin.get("modern-effect-font", BitmapFont.class);
            case CHAOS -> skin.get("chaos-effect-font", BitmapFont.class);
        };

        Label.LabelStyle label = skin.get("default", Label.LabelStyle.class);
        label.font = bodyFont;
        label.fontColor = textColor;
        Label.LabelStyle title = skin.get("title", Label.LabelStyle.class);
        title.font = titleFont;
        title.fontColor = textColor;
        Label.LabelStyle effect = skin.get("effect-text", Label.LabelStyle.class);
        effect.font = effectFont;
        effect.fontColor = textColor;

        TextButton.TextButtonStyle button = skin.get("default", TextButton.TextButtonStyle.class);
        button.font = bodyFont;
        button.up = skin.newDrawable("button");
        button.over = skin.newDrawable("button-over");
        button.down = skin.newDrawable("button-down");
        button.checked = skin.newDrawable("button-down");
        button.fontColor = textColor;
        button.overFontColor = Color.WHITE;
        button.downFontColor = Color.WHITE;
        button.checkedFontColor = Color.WHITE;

        TextField.TextFieldStyle input = skin.get("default", TextField.TextFieldStyle.class);
        input.font = bodyFont;
        input.fontColor = textColor;
        input.cursor = skin.newDrawable("bar-hp");
        input.selection = skin.newDrawable("button-over");
        input.background = skin.newDrawable("bar-bg");
    }

    /**
     * Skinのスタイル生成に先立ち、選択テーマの共通Drawable名を用意する。
     */
    static void installDrawables(Skin skin, UiTheme theme) {
        alias(skin, "menu-green", theme.resource("background"));
        alias(skin, "panel", theme.resource("panel"));
        alias(skin, "button", theme.resource("button"));
        alias(skin, "button-over", theme.resource("button-over"));
        alias(skin, "button-down", theme.resource("button-down"));
        alias(skin, "bar-bg", theme.resource("bar-bg"));
        alias(skin, "bar-hp", theme.resource("bar-hp"));
        alias(skin, "gauge-empty", theme.resource("gauge-empty"));
        alias(skin, "gauge-hp", theme.resource("gauge-hp"));
        alias(skin, "gauge-ultimate", theme.resource("gauge-ultimate"));
        alias(skin, "gauge-skill", theme.resource("gauge-skill"));
        alias(skin, "gauge-support-ultimate", theme.resource("gauge-support-ultimate"));
        alias(skin, "gauge-support-skill", theme.resource("gauge-support-skill"));
    }

    private static Preferences preferences() {
        return Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    private static void alias(Skin skin, String alias, String source) {
        skin.add(alias, skin.newDrawable(source), Drawable.class);
    }
}
