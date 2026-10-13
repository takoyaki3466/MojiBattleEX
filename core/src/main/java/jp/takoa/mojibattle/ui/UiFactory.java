package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import jp.takoa.mojibattle.i18n.LocalizationManager;

/** 日本語フォントと共通Drawableを生成し、Scene2D用Skinへ登録するUIリソースファクトリ。 */
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
        generator.dispose();
        skin.add("default-font", font);
        skin.add("menu-background", new Texture(Gdx.files.internal("ui/menu-background.png")));
        skin.add("menu-green", texture(new Color(.015f, .075f, .052f, 1f)));
        skin.add("panel", texture(new Color(.025f, .14f, .095f, .96f)));
        skin.add("button", texture(new Color(.045f, .34f, .19f, .98f)));
        skin.add("button-over", texture(new Color(.12f, .68f, .34f, 1f)));
        skin.add("button-down", texture(new Color(.025f, .24f, .13f, 1f)));
        skin.add("bar-bg", texture(new Color(.055f, .20f, .12f, .98f)));
        skin.add("bar-hp", texture(new Color(.30f, .94f, .40f, 1)));
        skin.add("gauge-empty", texture(new Color(.08f, .11f, .10f, .96f)));
        skin.add("gauge-hp", texture(new Color(.18f, .95f, .34f, 1f)));
        skin.add("gauge-ultimate", texture(new Color(1f, .72f, .08f, 1f)));
        skin.add("gauge-skill", texture(new Color(.12f, .78f, 1f, 1f)));
        skin.add("gauge-support-ultimate", texture(new Color(1f, .32f, .82f, 1f)));
        skin.add("gauge-support-skill", texture(new Color(.54f, .92f, .22f, 1f)));
        Label.LabelStyle label = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", label);
        skin.add("title", new Label.LabelStyle(titleFont, Color.WHITE));
        skin.add("effect-text", new Label.LabelStyle(effectTextFont, Color.WHITE));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle(
                skin.newDrawable("button"),
                skin.newDrawable("button-down"),
                skin.newDrawable("button-down"),
                font);
        button.over = skin.newDrawable("button-over");
        button.fontColor = Color.WHITE;
        button.overFontColor = Color.WHITE;
        button.downFontColor = Color.WHITE;
        skin.add("default", button);
        TextField.TextFieldStyle input = new TextField.TextFieldStyle(
                font,
                Color.WHITE,
                skin.newDrawable("bar-hp"),
                skin.newDrawable("button-over"),
                skin.newDrawable("bar-bg"));
        skin.add("default", input);
        return skin;
    }

    private static String unique(String input) {
        StringBuilder result = new StringBuilder();
        input.codePoints().distinct().forEach(result::appendCodePoint);
        return result.toString();
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
