package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public final class ScreenManager {
    private final Game game;
    public ScreenManager(Game game) { this.game = game; }
    public void show(Screen next) {
        Screen previous = game.getScreen();
        game.setScreen(next);
        if (previous != null) previous.dispose();
    }
}
