package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import jp.takoa.mojibattle.log.DiagnosticLog;

/**
 * libGDXの現在画面を置き換え、置換前の画面を確実に破棄する。
 */
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
        try {
            game.setScreen(next);
        } catch (RuntimeException | Error error) {
            rollback(previous, next, error);
            throw error;
        }
        DiagnosticLog.setCurrentScreen(next.getClass().getSimpleName());
        if (previous != null) {
            try {
                previous.dispose();
            } catch (RuntimeException | Error error) {
                DiagnosticLog.error("Previous screen disposal failed", error);
            }
        }
    }

    private void rollback(Screen previous, Screen failed, Throwable failure) {
        try {
            if (game.getScreen() != previous) {
                game.setScreen(previous);
            }
            DiagnosticLog.setCurrentScreen(previous == null ? "none" : previous.getClass().getSimpleName());
        } catch (RuntimeException | Error rollbackError) {
            failure.addSuppressed(rollbackError);
        }
        if (failed != previous) {
            try {
                failed.dispose();
            } catch (RuntimeException | Error disposeError) {
                failure.addSuppressed(disposeError);
            }
        }
    }
}
