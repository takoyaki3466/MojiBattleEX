package jp.takoa.mojibattle.debug;

import com.badlogic.gdx.Preferences;

/**
 * デバッグモードのON/OFFを既存のユーザー設定（Preferences）へ保存し、実行中の値を保持する。
 *
 * <p>値はGLスレッドで更新し、クラッシュレポートなど他スレッドからも読み取れるよう volatile で保持する。
 * 既定値はOFF。</p>
 */
public final class DebugSettings {
    /** {@link jp.takoa.mojibattle.ui.UiThemeManager#PREFERENCES_NAME} 内の保存キー。 */
    public static final String PREFERENCE_KEY = "debugMode";

    private static volatile boolean enabled;

    private DebugSettings() {
    }

    /**
     * 保存済みの値を読み込み、実行中の値へ反映する。
     */
    public static boolean load(Preferences preferences) {
        enabled = preferences.getBoolean(PREFERENCE_KEY, false);
        return enabled;
    }

    /**
     * 値を保存し、再起動せずに即時反映する。
     */
    public static void save(Preferences preferences, boolean value) {
        preferences.putBoolean(PREFERENCE_KEY, value).flush();
        enabled = value;
    }

    /** 現在デバッグモードが有効か。 */
    public static boolean enabled() {
        return enabled;
    }
}
