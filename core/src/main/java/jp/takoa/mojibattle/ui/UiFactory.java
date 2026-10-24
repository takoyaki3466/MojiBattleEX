package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import jp.takoa.mojibattle.i18n.LocalizationManager;

/**
 * 日本語フォントと共通Drawableを生成し、Scene2D用Skinへ登録するUIリソースファクトリ。
 */
public final class UiFactory {
    private static final String FONT_PATH = "fonts/NotoSansJP-VF.ttf";

    private UiFactory() {
    }

    /**
     * 翻訳文と追加文字を収録したフォント、および画面共通スタイルを生成する。
     *
     * @return 呼び出し側が破棄責任を持つSkin
     */
    public static Skin createSkin(LocalizationManager i18n, String additionalCharacters) {
        Skin skin = new Skin();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 28;

        parameter.characters = unique(FreeTypeFontGenerator.DEFAULT_CHARS + i18n.allCharacters() + additionalCharacters);
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        BitmapFont font = generator.generateFont(parameter);

        FreeTypeFontGenerator.FreeTypeFontParameter titleParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParameter.size = 88;
        titleParameter.characters = parameter.characters;
        titleParameter.color = Color.WHITE;
        titleParameter.borderWidth = 2.5f;
        titleParameter.borderColor = Color.WHITE;
        titleParameter.minFilter = Texture.TextureFilter.Linear;
        titleParameter.magFilter = Texture.TextureFilter.Linear;

        BitmapFont titleFont = generator.generateFont(titleParameter);

        FreeTypeFontGenerator.FreeTypeFontParameter effectTextParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        effectTextParameter.size = 44;
        effectTextParameter.characters = parameter.characters;
        effectTextParameter.color = Color.WHITE;
        effectTextParameter.borderWidth = 3f;
        effectTextParameter.borderColor = Color.BLACK;
        effectTextParameter.minFilter = Texture.TextureFilter.Linear;
        effectTextParameter.magFilter = Texture.TextureFilter.Linear;

        BitmapFont effectTextFont = generator.generateFont(effectTextParameter);
        BitmapFont modernFont = themedFont(generator, parameter.characters, 28, Color.WHITE, 1f, new Color(.04f, .30f, .34f, 1f), 1, 1, new Color(0f, 0f, 0f, .8f));
        BitmapFont modernTitleFont = themedFont(generator, parameter.characters, 88, Color.WHITE, 2f, new Color(.08f, .72f, .76f, 1f), 2, 2, new Color(0f, 0f, 0f, .9f));
        BitmapFont modernEffectFont = themedFont(generator, parameter.characters, 44, Color.WHITE, 1.5f, new Color(.08f, .72f, .76f, 1f), 1, 2, new Color(0f, 0f, 0f, .9f));
        BitmapFont chaosFont = themedFont(generator, parameter.characters, 28, Color.WHITE, 3f, new Color(.04f, 0f, .10f, 1f), 3, 2, new Color(1f, 0f, .62f, 1f));
        BitmapFont chaosTitleFont = themedFont(generator, parameter.characters, 88, new Color(1f, .96f, .08f, 1f), 5.5f, new Color(.04f, 0f, .10f, 1f), 5, 4, new Color(0f, .92f, 1f, 1f));
        BitmapFont chaosEffectFont = themedFont(generator, parameter.characters, 44, Color.WHITE, 4f, new Color(.04f, 0f, .10f, 1f), 4, 3, new Color(1f, 0f, .62f, 1f));
        generator.dispose();

        skin.add("default-font", font);
        skin.add("default-title-font", titleFont);
        skin.add("default-effect-font", effectTextFont);
        skin.add("modern-font", modernFont);
        skin.add("modern-title-font", modernTitleFont);
        skin.add("modern-effect-font", modernEffectFont);
        skin.add("chaos-font", chaosFont);
        skin.add("chaos-title-font", chaosTitleFont);
        skin.add("chaos-effect-font", chaosEffectFont);
        skin.add("menu-background", new Texture(Gdx.files.internal("ui/menu-background.png")));
        registerThemes(skin);
        UiTheme selectedTheme = UiThemeManager.selected();
        UiThemeManager.installDrawables(skin, selectedTheme);

        Label.LabelStyle label = new Label.LabelStyle(font, Color.WHITE);

        skin.add("default", label);
        skin.add("title", new Label.LabelStyle(titleFont, Color.WHITE));
        skin.add("effect-text", new Label.LabelStyle(effectTextFont, Color.WHITE));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle(skin.newDrawable("button"), skin.newDrawable("button-down"), skin.newDrawable("button-down"), font);

        button.over = skin.newDrawable("button-over");
        button.fontColor = Color.WHITE;
        button.overFontColor = Color.WHITE;
        button.downFontColor = Color.WHITE;

        skin.add("default", button);

        TextField.TextFieldStyle input = new TextField.TextFieldStyle(font, Color.WHITE, skin.newDrawable("bar-hp"), skin.newDrawable("button-over"), skin.newDrawable("bar-bg"));

        skin.add("default", input);
        UiThemeManager.apply(skin, selectedTheme);
        return skin;
    }

    private static void registerThemes(Skin skin) {
        registerTheme(skin, UiTheme.CURRENT, texture(new Color(.015f, .075f, .052f, 1f)), new Color(.025f, .14f, .095f, .96f), new Color(.045f, .34f, .19f, .98f), new Color(.12f, .68f, .34f, 1f), new Color(.025f, .24f, .13f, 1f), new Color(.055f, .20f, .12f, .98f), new Color(.30f, .94f, .40f, 1f), new Color(.08f, .11f, .10f, .96f), new Color(.18f, .95f, .34f, 1f), new Color(1f, .72f, .08f, 1f), new Color(.12f, .78f, 1f, 1f), new Color(1f, .32f, .82f, 1f), new Color(.54f, .92f, .22f, 1f));

        registerTheme(skin, UiTheme.MODERN, new Texture(Gdx.files.internal("ui/theme-modern-background.png")), new Color(.025f, .12f, .16f, .96f), new Color(.04f, .20f, .24f, .98f), new Color(.05f, .42f, .44f, 1f), new Color(.42f, .16f, .14f, 1f), new Color(.035f, .15f, .19f, .98f), new Color(.96f, .48f, .34f, 1f), new Color(.025f, .08f, .12f, .96f), new Color(.20f, .94f, .76f, 1f), new Color(1f, .72f, .24f, 1f), new Color(.18f, .86f, .90f, 1f), new Color(.72f, .58f, 1f, 1f), new Color(.48f, 1f, .82f, 1f));

        registerTheme(skin, UiTheme.CHAOS, new Texture(Gdx.files.internal("ui/theme-chaos-background-v2.png")), new Color(.10f, .015f, .22f, .94f), new Color(.72f, .015f, .48f, .98f), new Color(.02f, .74f, .90f, 1f), new Color(.92f, .32f, .02f, 1f), new Color(.12f, .015f, .25f, .98f), new Color(1f, .08f, .56f, 1f), new Color(.025f, .01f, .12f, .96f), new Color(.54f, 1f, .02f, 1f), new Color(1f, .92f, .02f, 1f), new Color(.02f, .94f, 1f, 1f), new Color(1f, .02f, .72f, 1f), new Color(.68f, 1f, .02f, 1f));

        registerModernFrames(skin);
        registerChaosFrames(skin);
        registerChaosControls(skin);
    }

    private static void registerModernFrames(Skin skin) {
        framedDrawable(skin, UiTheme.MODERN.resource("panel"), 128, 128, 10, new Color(.018f, .075f, .11f, .96f), new Color(.08f, .58f, .64f, 1f), new Color(1f, .48f, .28f, 1f), false);
        framedDrawable(skin, UiTheme.MODERN.resource("button"), 96, 48, 9, new Color(.025f, .13f, .17f, .99f), new Color(.08f, .55f, .62f, 1f), new Color(.22f, .92f, .88f, 1f), false);
        framedDrawable(skin, UiTheme.MODERN.resource("button-over"), 96, 48, 9, new Color(.035f, .25f, .29f, 1f), new Color(.25f, .98f, .94f, 1f), new Color(1f, .72f, .24f, 1f), false);
        framedDrawable(skin, UiTheme.MODERN.resource("button-down"), 96, 48, 9, new Color(.30f, .075f, .075f, 1f), new Color(1f, .40f, .28f, 1f), new Color(1f, .78f, .30f, 1f), false);
    }

    private static void registerChaosFrames(Skin skin) {
        chaosFramedDrawable(skin, UiTheme.CHAOS.resource("panel"), 128, 128, 14, new Color(.07f, .005f, .16f, .94f), new Color(1f, .04f, .68f, 1f), new Color(.02f, .95f, 1f, 1f), new Color(1f, .92f, .02f, 1f));
        chaosFramedDrawable(skin, UiTheme.CHAOS.resource("button"), 96, 48, 11, new Color(.28f, .005f, .34f, .99f), new Color(1f, .04f, .68f, 1f), new Color(1f, .92f, .02f, 1f), new Color(.02f, .95f, 1f, 1f));
        chaosFramedDrawable(skin, UiTheme.CHAOS.resource("button-over"), 96, 48, 11, new Color(.005f, .32f, .38f, 1f), new Color(.02f, .95f, 1f, 1f), new Color(.68f, 1f, .02f, 1f), new Color(1f, .04f, .68f, 1f));
        chaosFramedDrawable(skin, UiTheme.CHAOS.resource("button-down"), 96, 48, 11, new Color(.48f, .08f, .005f, 1f), new Color(1f, .86f, .02f, 1f), new Color(1f, .04f, .68f, 1f), new Color(.02f, .95f, 1f, 1f));
    }

    /**
     * UI 3専用ボタンとキャラクターカードの色違いDrawableをまとめて登録する。
     */
    private static void registerChaosControls(Skin skin) {
        Color[] accents = {new Color(1f, .02f, .66f, 1f), new Color(.02f, .92f, 1f, 1f), new Color(1f, .92f, .02f, 1f), new Color(.62f, 1f, .02f, 1f), new Color(1f, .30f, .015f, 1f), new Color(.72f, .76f, .88f, 1f)};
        String[] names = {"magenta", "cyan", "yellow", "lime", "orange", "steel"};

        for (int index = 0; index < accents.length; index++) {
            Color accent = accents[index];
            String name = "chaos-action-" + names[index];
            chaosButtonDrawable(skin, name, 160, 64, 20, 12, shade(accent, .24f), accent, index % 2 == 0 ? accents[1] : accents[2]);
            chaosButtonDrawable(skin, name + "-over", 160, 64, 20, 12, shade(accent, .42f), Color.WHITE, accent);
            chaosButtonDrawable(skin, name + "-down", 160, 64, 20, 12, shade(accent, .62f), accents[2], accents[0]);

            chaosButtonDrawable(skin, "chaos-card-" + index, 96, 96, 14, 14, shade(accent, .28f), accent, index % 2 == 0 ? accents[2] : accents[1]);
        }
        chaosButtonDrawable(skin, "chaos-card-selected", 96, 96, 14, 14, new Color(.42f, .015f, .20f, .99f), Color.WHITE, accents[2]);
    }

    private static void registerTheme(Skin skin, UiTheme theme, Texture background, Color panel, Color button, Color buttonOver, Color buttonDown, Color barBackground, Color barHp, Color gaugeEmpty, Color gaugeHp, Color gaugeUltimate, Color gaugeSkill, Color supportUltimate, Color supportSkill) {
        skin.add(theme.resource("background"), background);
        skin.add(theme.resource("panel"), texture(panel));
        skin.add(theme.resource("button"), texture(button));
        skin.add(theme.resource("button-over"), texture(buttonOver));
        skin.add(theme.resource("button-down"), texture(buttonDown));
        skin.add(theme.resource("bar-bg"), texture(barBackground));
        skin.add(theme.resource("bar-hp"), texture(barHp));
        skin.add(theme.resource("gauge-empty"), texture(gaugeEmpty));
        skin.add(theme.resource("gauge-hp"), texture(gaugeHp));
        skin.add(theme.resource("gauge-ultimate"), texture(gaugeUltimate));
        skin.add(theme.resource("gauge-skill"), texture(gaugeSkill));
        skin.add(theme.resource("gauge-support-ultimate"), texture(supportUltimate));
        skin.add(theme.resource("gauge-support-skill"), texture(supportSkill));
    }

    private static String unique(String input) {
        StringBuilder result = new StringBuilder();
        input.codePoints().distinct().forEach(result::appendCodePoint);

        return result.toString();
    }

    private static BitmapFont themedFont(FreeTypeFontGenerator generator, String characters, int size, Color color, float borderWidth, Color borderColor, int shadowOffsetX, int shadowOffsetY, Color shadowColor) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = characters;
        parameter.color = color;
        parameter.borderWidth = borderWidth;
        parameter.borderColor = borderColor;
        parameter.shadowOffsetX = shadowOffsetX;
        parameter.shadowOffsetY = shadowOffsetY;
        parameter.shadowColor = shadowColor;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(parameter);
    }

    private static void framedDrawable(Skin skin, String name, int width, int height, int split, Color fill, Color border, Color accent, boolean noisy) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fill();

        for (int inset = 0; inset < 3; inset++) {
            pixmap.setColor(inset == 1 ? accent : border);
            pixmap.drawRectangle(inset, inset, width - inset * 2, height - inset * 2);
        }

        pixmap.setColor(accent);
        pixmap.fillRectangle(4, 7, 5, height - 14);
        pixmap.fillRectangle(width - 10, 7, 4, height - 14);

        if (noisy) {
            for (int x = 12; x < width - 12; x += 16) {
                pixmap.setColor((x / 16) % 2 == 0 ? accent : border);
                pixmap.fillRectangle(x, 4, 9, 4);
                pixmap.fillRectangle(width - x - 9, height - 8, 9, 4);
            }
            pixmap.setColor(new Color(.68f, 1f, .02f, 1f));
            pixmap.drawLine(10, height - 11, 24, height - 4);
            pixmap.drawLine(width - 25, 4, width - 10, 12);
        } else {
            pixmap.setColor(accent);
            pixmap.drawLine(12, height - 6, width / 3, height - 6);
            pixmap.drawLine(width * 2 / 3, 5, width - 12, 5);
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        skin.add(name + "-texture", texture);
        skin.add(name, new NinePatchDrawable(new NinePatch(texture, split, split, split, split)), Drawable.class);
    }

    /**
     * CHAOS専用の多重ネオン枠。装飾をNinePatchの固定端へ閉じ込め、伸縮する中央の可読性は保つ。
     */
    private static void chaosFramedDrawable(Skin skin, String name, int width, int height, int split, Color fill, Color border, Color accent, Color spark) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fill();

        // 黒い外周と交互色のネオン線を重ね、参考UIの厚い縁取りを小さなNinePatchで再現する。
        pixmap.setColor(new Color(.015f, 0f, .035f, 1f));
        pixmap.fillRectangle(0, 0, width, 3);
        pixmap.fillRectangle(0, height - 3, width, 3);
        pixmap.fillRectangle(0, 0, 3, height);
        pixmap.fillRectangle(width - 3, 0, 3, height);
        for (int inset = 2; inset <= 7; inset++) {
            pixmap.setColor((inset & 1) == 0 ? border : accent);
            pixmap.drawRectangle(inset, inset, width - inset * 2, height - inset * 2);
        }

        // 上下は警告帯、左右は色の違う発光ブロック。すべて固定端の内側に収める。
        int stripeWidth = Math.max(6, split - 3);
        for (int x = split; x < width - split; x += stripeWidth) {
            boolean alternate = ((x - split) / stripeWidth & 1) == 0;
            pixmap.setColor(alternate ? spark : new Color(.025f, .005f, .055f, 1f));
            pixmap.fillRectangle(x, 4, Math.min(stripeWidth, width - split - x), 3);
            pixmap.setColor(alternate ? border : accent);
            pixmap.fillRectangle(x, height - 7, Math.min(stripeWidth, width - split - x), 3);
        }
        pixmap.setColor(accent);
        pixmap.fillRectangle(4, 10, 4, height - 20);
        pixmap.setColor(border);
        pixmap.fillRectangle(width - 8, 10, 4, height - 20);

        // 角の稲妻・スラッシュ・火花。中央へ侵入させず、ラベル文字との競合を避ける。
        pixmap.setColor(spark);
        pixmap.drawLine(7, split, split - 1, 7);
        pixmap.drawLine(7, split + 4, split + 4, 7);
        pixmap.drawLine(width - 8, height - split, width - split, height - 8);
        pixmap.drawLine(width - 8, height - split - 4, width - split - 4, height - 8);
        pixmap.fillRectangle(9, 9, 3, 3);
        pixmap.fillRectangle(width - 12, height - 12, 3, 3);
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(10, 10);
        pixmap.drawPixel(width - 11, height - 11);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        skin.add(name + "-texture", texture);
        skin.add(name, new NinePatchDrawable(new NinePatch(texture, split, split, split, split)), Drawable.class);
    }

    /**
     * 透明な切り欠きと左右の矢尻を持つ、伸縮可能なアーケード風ボタンを生成する。
     */
    private static void chaosButtonDrawable(Skin skin, String name, int width, int height, int horizontalSplit, int verticalSplit, Color fill, Color border, Color accent) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        int point = horizontalSplit - 3;
        int middle = height / 2;

        pixmap.setColor(fill);
        pixmap.fillRectangle(point, 2, width - point * 2, height - 4);
        pixmap.fillTriangle(1, middle, point, 2, point, height - 3);
        pixmap.fillTriangle(width - 2, middle, width - point - 1, height - 3, width - point - 1, 2);

        // 黒い影、白いハイライト、主色の順で輪郭を重ねる。
        Color shadow = new Color(.015f, 0f, .035f, 1f);
        drawChevron(pixmap, width, height, point, 0, shadow);
        drawChevron(pixmap, width, height, point, 2, border);
        drawChevron(pixmap, width, height, point, 4, accent);
        pixmap.setColor(Color.WHITE);
        pixmap.drawLine(point + 5, 6, width - point - 6, 6);

        // 警告帯と速度線は伸縮しない左右端へ限定する。
        for (int y = 10; y < height - 10; y += 9) {
            pixmap.setColor((y / 9 & 1) == 0 ? accent : shadow);
            pixmap.drawLine(5, middle, point - 3, y);
            pixmap.drawLine(width - 6, middle, width - point + 2, height - y - 1);
        }
        pixmap.setColor(Color.WHITE);
        pixmap.fillRectangle(point - 2, middle - 1, 3, 3);
        pixmap.fillRectangle(width - point - 1, middle - 1, 3, 3);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        skin.add(name + "-texture", texture);
        skin.add(name, new NinePatchDrawable(new NinePatch(texture, horizontalSplit, horizontalSplit, verticalSplit, verticalSplit)), Drawable.class);
    }

    private static void drawChevron(Pixmap pixmap, int width, int height, int point, int inset, Color color) {
        pixmap.setColor(color);
        pixmap.drawLine(1 + inset, height / 2, point + inset, 2 + inset);
        pixmap.drawLine(point + inset, 2 + inset, width - point - inset - 1, 2 + inset);
        pixmap.drawLine(width - point - inset - 1, 2 + inset, width - inset - 2, height / 2);
        pixmap.drawLine(width - inset - 2, height / 2, width - point - inset - 1, height - inset - 3);
        pixmap.drawLine(width - point - inset - 1, height - inset - 3, point + inset, height - inset - 3);
        pixmap.drawLine(point + inset, height - inset - 3, 1 + inset, height / 2);
    }

    private static Color shade(Color color, float amount) {
        return new Color(color.r * amount, color.g * amount, color.b * amount, .98f);
    }

    private static Texture texture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return texture;
    }
}
