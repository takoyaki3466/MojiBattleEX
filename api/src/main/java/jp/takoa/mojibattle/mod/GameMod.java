package jp.takoa.mojibattle.mod;

/** 外部JARまたは標準コンテンツJARが実装するMODエントリーポイント。 */
public interface GameMod {
    /** @return metadataと一致する小文字のMOD ID */
    String id();

    /**
     * Registry登録宣言をライフサイクルバスへ接続する。
     * この段階で登録済みHandleを解決したりゲーム状態を生成してはならない。
     *
     * @param context このMOD専用に制限された登録コンテキスト
     */
    void register(ModContext context);

    /**
     * データ解決とRegistry凍結後に一度だけ呼ばれる初期化フック。
     * 初期実装では追加登録を受け付けない。
     */
    default void initialize(ModContext context) {
    }
}
