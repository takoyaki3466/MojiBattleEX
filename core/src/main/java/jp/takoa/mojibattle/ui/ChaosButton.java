package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

/**
 * UI 3専用の、色別ネオン枠と矢印形状を持つアクションボタン。
 */
public final class ChaosButton extends TextButton {
    public enum Accent {
        MAGENTA("magenta"), CYAN("cyan"), YELLOW("yellow"), LIME("lime"), ORANGE("orange"), STEEL("steel");

        private final String resourceName;

        Accent(String resourceName) {
            this.resourceName = resourceName;
        }
    }

    public ChaosButton(String text, Skin skin, Accent accent) {
        super(text, style(skin, accent));
        getLabel().setAlignment(Align.center);
        pad(8, 34, 10, 34);
    }

    /**
     * キャラクターカードをIDごとに安定したネオン色へ振り分ける。
     */
    public static String cardBackground(String id) {
        return "chaos-card-" + Math.floorMod(id.hashCode(), Accent.values().length);
    }

    public static String selectedCardBackground() {
        return "chaos-card-selected";
    }

    private static TextButtonStyle style(Skin skin, Accent accent) {
        TextButtonStyle style = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
        String prefix = "chaos-action-" + accent.resourceName;
        style.up = skin.newDrawable(prefix);
        style.over = skin.newDrawable(prefix + "-over");
        style.down = skin.newDrawable(prefix + "-down");
        style.checked = style.down;
        return style;
    }
}
