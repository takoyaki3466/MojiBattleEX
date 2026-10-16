package jp.takoa.mojibattle.content;

/**
 * MODがcore型へ依存せず、データ駆動コンテンツを宣言する登録境界。
 */
public interface ContentRegistrar {
    /**
     * キャラクターと3種類のAbility定義を一つの原子的な登録単位として宣言する。
     */
    void registerCharacter(CharacterDataRegistration registration);

    /** 外部補助キャラクターのJSONと翻訳辞書を宣言する。 */
    default void registerSupportCharacter(SupportCharacterDataRegistration registration) {
        throw new UnsupportedOperationException("Support character registration is not supported by this host");
    }

    /** 外部フィールド効果JSONを宣言する。 */
    default void registerFieldEffect(FieldEffectDataRegistration registration) {
        throw new UnsupportedOperationException("Field effect registration is not supported by this host");
    }
}
