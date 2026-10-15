package jp.takoa.mojibattle;

import com.badlogic.gdx.Game;
import jp.takoa.mojibattle.screen.ScreenManager;
import jp.takoa.mojibattle.screen.TitleScreen;

import java.util.function.Supplier;

/**
 * libGDXアプリケーションのライフサイクルをゲーム固有の画面管理へ接続するエントリーポイント。
 *
 * <p>生成時に共有コンテキストとタイトル画面を準備し、終了時にはコンテキストが所有する
 * UIリソースを解放する。</p>
 */
public final class GameApplication extends Game {
    private final Supplier<GameContext> contextFactory;
    private GameContext context;

    public GameApplication(Supplier<GameContext> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public void create() {
        context = contextFactory.get();

        ScreenManager screens = new ScreenManager(this);
        screens.show(new TitleScreen(context, screens));
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
