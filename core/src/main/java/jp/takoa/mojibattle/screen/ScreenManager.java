package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/** libGDXの現在画面を置き換え、置換前の画面を確実に破棄する。 */
public final class ScreenManager {
    private final Game game;

    public ScreenManager(Game game) {
        this.game = game;
    }

    /**
     * 次の画面へ切り替え、直前の画面があれば破棄する。
     *
     * @param next 表示する画面
     */
    public void show(Screen next) {
        Screen previous = game.getScreen();
        game.setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }
}
