package jp.takoa.mojibattle.battle;

/**
 * MODへ公開する戦闘参加者の読取り専用ビュー。
 */
public interface CombatantView {
    String id();

    float hp();

    float maximumHp();

    float ultimatePoints();

    /** 必殺ゲージ上限。旧ホストでは100を既定値とする。 */
    default float maximumUltimatePoints() {
        return 100f;
    }

    boolean alive();
}
