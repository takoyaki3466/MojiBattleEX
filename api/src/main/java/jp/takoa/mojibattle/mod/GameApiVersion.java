package jp.takoa.mojibattle.mod;

/**
 * loaderとMOD metadataが共有する公開互換性バージョン。
 *
 * <p>APIバージョンは {@code メジャー.マイナー.パッチ} の3つで表す。</p>
 * <ul>
 *     <li>メジャー: 既存MODとの互換性がなくなる変更で上げる。MODの {@code apiVersion} とメジャーが異なる場合は読み込まない。</li>
 *     <li>マイナー: 互換性を保ったままAPIを追加したときに上げる。</li>
 *     <li>パッチ: APIの形を変えない修正で上げる。</li>
 * </ul>
 * <p>MODは {@code mod.json} の {@code apiVersion} に、開発に使ったAPIバージョンを書く。
 * ゲーム側のAPIバージョンがそれ以上（同じメジャー内）であれば読み込める。</p>
 */
public final class GameApiVersion {
    /** APIのメジャーバージョン。 */
    public static final int API_MAJOR = 1;
    /** APIのマイナーバージョン。 */
    public static final int API_MINOR = 1;
    /** APIのパッチバージョン。 */
    public static final int API_PATCH = 0;
    /** {@code メジャー.マイナー.パッチ} 形式のAPIバージョン（例: {@code 1.1.0}）。 */
    public static final String API_VERSION = API_MAJOR + "." + API_MINOR + "." + API_PATCH;
    /** MOD metadataの {@code gameVersion} と依存指定 {@code mojibattle_game} で比較するゲーム本体の互換性バージョン。 */
    public static final String GAME_VERSION = "1.1.0";

    private GameApiVersion() {
    }
}
