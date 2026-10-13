package jp.takoa.mojibattle.domain.ability;

/** IDによって登録・参照される能力の共通契約。 */
public interface IAbility {
    /** @return 設定およびキャラクター定義から参照される一意なID */
    String id();
}
