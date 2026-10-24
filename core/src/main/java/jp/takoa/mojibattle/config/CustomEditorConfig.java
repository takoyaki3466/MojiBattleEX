package jp.takoa.mojibattle.config;

/**
 * カスタムキャラクター編集機能の保存枠数と選択上限を保持する設定DTO。
 */
public final class CustomEditorConfig {
    public int maximumSlots = 10;
    public int maximumMainPassives = 3;
    public int maximumSupportFieldPassives = 2;
    public int saveVersion = 1;
}
