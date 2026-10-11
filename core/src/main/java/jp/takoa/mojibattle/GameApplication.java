package jp.takoa.mojibattle;

import com.badlogic.gdx.Game;
import jp.takoa.mojibattle.screen.ScreenManager;
import jp.takoa.mojibattle.screen.TitleScreen;

public final class GameApplication extends Game {
    private GameContext context;
    @Override public void create() {
        context = new GameContext();
        ScreenManager screens = new ScreenManager(this);
        screens.show(new TitleScreen(context, screens));
    }
    @Override public void dispose() {
        super.dispose();
        if (context != null) context.close();
    }
}
