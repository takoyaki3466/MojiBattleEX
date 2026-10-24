package jp.takoa.mojibattle.input;

import com.badlogic.gdx.Preferences;

import java.util.Locale;

/**
 * 全プレイヤーに共通して適用する入力方式。
 */
public enum InputDeviceMode {
    KEYBOARD,
    CONTROLLER;

    public static final String PREFERENCE_KEY = "inputDeviceMode";

    /**
     * 保存値が壊れている場合も従来どおりキーボードへ安全に戻す。
     */
    public static InputDeviceMode load(Preferences preferences) {
        String value = preferences.getString(PREFERENCE_KEY, KEYBOARD.name());
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return KEYBOARD;
        }
    }

    public void save(Preferences preferences) {
        preferences.putString(PREFERENCE_KEY, name()).flush();
    }

    public InputDeviceMode next() {
        return this == KEYBOARD ? CONTROLLER : KEYBOARD;
    }
}
