package jp.takoa.mojibattle.log;

import com.badlogic.gdx.Gdx;

/**
 * libGDXのロガーへカテゴリ付きメッセージを渡す薄い窓口。
 */
public final class GameLog {
    private GameLog() {
    }

    public static void info(String category, String message) {
        Gdx.app.log(category, message);
    }

    public static void error(String category, String message, Throwable error) {
        Gdx.app.error(category, message, error);
    }
}
