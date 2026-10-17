package jp.takoa.mojibattle.ability;

/** 補助Abilityの所有個体と専用ゲージを表す制御用ビュー。HP等の戦闘値はBattlefieldViewから取得する。 */
public interface SupportView {
    String id();

    /** 戦闘内で一意な個体ID。同じ定義を複数編成しても異なる。 */
    default String instanceId() { return id(); }

    float ultimatePoints();

    float maximumUltimatePoints();
}
