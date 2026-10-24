package jp.takoa.mojibattle.ui;

/**
 * ボタン配置とは独立して切り替えられるUIの外観テーマ。
 */
public enum UiTheme {
    CURRENT(1, "settings.ui_theme.current", "theme-current"),
    MODERN(2, "settings.ui_theme.modern", "theme-modern"),
    CHAOS(3, "settings.ui_theme.chaos", "theme-chaos");

    private final int id;
    private final String translationKey;
    private final String resourcePrefix;

    UiTheme(int id, String translationKey, String resourcePrefix) {
        this.id = id;
        this.translationKey = translationKey;
        this.resourcePrefix = resourcePrefix;
    }

    public int id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    String resource(String component) {
        return resourcePrefix + '-' + component;
    }

    /** 次のテーマへ循環する。 */
    public UiTheme next() {
        UiTheme[] themes = values();
        return themes[(ordinal() + 1) % themes.length];
    }

    /** 保存値が不正な場合は従来テーマへ戻す。 */
    public static UiTheme fromId(int id) {
        for (UiTheme theme : values()) {
            if (theme.id == id) {
                return theme;
            }
        }
        return CURRENT;
    }
}
