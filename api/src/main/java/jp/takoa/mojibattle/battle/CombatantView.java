package jp.takoa.mojibattle.battle;

/** MODへ公開する戦闘参加者の読取り専用ビュー。 */
public interface CombatantView {
    String id();

    float hp();

    float maximumHp();

    float ultimatePoints();

    boolean alive();
}
