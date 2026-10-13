package jp.takoa.mojibattle.config;

/** 言語・音量と、起動時に有効化する各種コンテンツIDを保持するゲーム設定DTO。 */
public final class GameConfig {
    public String language;
    public float uiScale;
    public float masterVolume;
    public String[] characterIds;
    public String[] supportIds;
    public String[] fieldEffectIds;
}
