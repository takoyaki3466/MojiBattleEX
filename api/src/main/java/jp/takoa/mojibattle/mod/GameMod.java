package jp.takoa.mojibattle.mod;

/**
 * MODのエントリーポイント。
 */
public interface GameMod {

    /**
     * MOD IDを返す。
     *
     * @return MOD ID
     */
    default String id() {
        ModEntry entry = getClass().getAnnotation(ModEntry.class);

        if (entry == null) {
            throw new IllegalStateException("Missing @ModEntry: " + getClass().getName());
        }

        return entry.value();
    }

    /**
     * MODの登録処理。
     *
     * @param context 登録コンテキスト
     */
    void register(ModContext context);

    /**
     * MODの初期化処理。
     *
     * @param context 初期化コンテキスト
     */
    default void initialize(ModContext context) {
    }
}