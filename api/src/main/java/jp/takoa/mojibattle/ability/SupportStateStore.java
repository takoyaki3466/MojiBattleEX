package jp.takoa.mojibattle.ability;

/** 戦闘中の補助キャラクター個体だけに属する型付き状態。戦闘終了後は利用できない。 */
public interface SupportStateStore {
    int getInt(String key, int fallback);
    void putInt(String key, int value);
    float getFloat(String key, float fallback);
    void putFloat(String key, float value);
    boolean getBoolean(String key, boolean fallback);
    void putBoolean(String key, boolean value);
    String getString(String key, String fallback);
    void putString(String key, String value);
    boolean contains(String key);
    void remove(String key);
}
