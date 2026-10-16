package jp.takoa.mojibattle.ability;

/** HPを持たない補助キャラクターの読取り専用ビュー。 */
public interface SupportView {
    String id();

    /** 戦闘内で一意な個体ID。同じ定義を複数編成しても異なる。 */
    default String instanceId() { return id(); }

    float ultimatePoints();

    float maximumUltimatePoints();
}
