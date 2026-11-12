package jp.takoa.mojibattle.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * デバッグモード時に全画面共通で左上へFPSを描画するオーバーレイ。
 *
 * <p>FPSはlibGDXが集計する {@link com.badlogic.gdx.Graphics#getFramesPerSecond()} をそのまま使い、
 * 独自の計測は行わない。表示文字列は値が変わったときだけ再生成する。
 * 描画資源はデバッグモードが最初に有効になった時点で生成する。</p>
 */
public final class DebugOverlay implements Disposable {
    private static final float MARGIN = 8f;

    private SpriteBatch batch;
    private BitmapFont font;
    private int shownFps = -1;
    private String fpsText = "";

    /**
     * 現在の画面描画の後に呼び出す。デバッグモードがOFFなら何もしない。
     */
    public void render() {
        if (!DebugSettings.enabled()) {
            return;
        }
        if (batch == null) {
            batch = new SpriteBatch(64);
            // libGDX同梱のASCIIフォント。FPS表示は英数字のみのため共有Skinのフォント状態を変更しない。
            font = new BitmapFont();
            font.getData().setScale(1.25f);
        }

        int fps = Gdx.graphics.getFramesPerSecond();
        if (fps != shownFps) {
            shownFps = fps;
            fpsText = "FPS: " + fps;
        }

        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        int logicalWidth = Gdx.graphics.getWidth();
        int logicalHeight = Gdx.graphics.getHeight();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, logicalWidth, logicalHeight);
        batch.begin();
        float x = MARGIN;
        float y = logicalHeight - MARGIN;
        font.setColor(Color.BLACK);
        font.draw(batch, fpsText, x + 1, y - 1);
        font.setColor(Color.YELLOW);
        font.draw(batch, fpsText, x, y);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}
