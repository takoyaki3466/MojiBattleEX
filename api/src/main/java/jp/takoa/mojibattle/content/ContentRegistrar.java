package jp.takoa.mojibattle.content;

/** MODがcore型へ依存せず、データ駆動コンテンツを宣言する登録境界。 */
public interface ContentRegistrar {
    /** キャラクターと3種類のAbility定義を一つの原子的な登録単位として宣言する。 */
    void registerCharacter(CharacterDataRegistration registration);
}
