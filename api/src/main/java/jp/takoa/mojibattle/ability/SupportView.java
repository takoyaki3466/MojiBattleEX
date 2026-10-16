package jp.takoa.mojibattle.ability;

/** HPを持たない補助キャラクターの読取り専用ビュー。 */
public interface SupportView {
    String id();

    float ultimatePoints();

    float maximumUltimatePoints();
}
