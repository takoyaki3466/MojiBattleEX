package jp.takoa.mojibattle.info;

/**
 * 情報画面に表示する、導入済みMODの1件分の情報。
 *
 * @param id      MOD ID
 * @param name    表示名。metadataが無い場合はID
 * @param version MODのバージョン。不明な場合は空文字
 * @param source  読込み元
 * @param file    外部MODのJARファイル名。標準MODでは空文字
 */
public record InstalledMod(String id, String name, String version, Source source, String file) {
    public InstalledMod {
        id = id == null ? "" : id;
        name = name == null || name.isBlank() ? id : name;
        version = version == null ? "" : version;
        source = source == null ? Source.BUILT_IN : source;
        file = file == null ? "" : file;
    }

    /** MODの読込み元。 */
    public enum Source {
        /** アプリケーションに同梱され、クラスパスから読み込んだMOD。 */
        BUILT_IN,
        /** modsフォルダのJARから読み込んだMOD。 */
        EXTERNAL
    }
}
