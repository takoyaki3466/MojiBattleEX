package jp.takoa.mojibattle.battle;

/**
 * MODへ公開する戦闘参加者の読取り専用ビュー。
 */
public interface CombatantView {
    String id();

    /** 戦闘中の個体を一意に表すID。旧ホストでは定義IDへフォールバックする。 */
    default String instanceId() {
        return id();
    }

    /** この個体がメイン、補助、召喚のどれであるかを返す。 */
    default CombatantKind kind() {
        return CombatantKind.MAIN;
    }

    float hp();

    float maximumHp();

    float ultimatePoints();

    /** 必殺ゲージ上限。旧ホストでは100を既定値とする。 */
    default float maximumUltimatePoints() {
        return 100f;
    }

    boolean alive();

    /** 防御を含む現在の被ダメージ倍率。1が無補正。 */
    default float defenseMultiplier() {
        return 1f;
    }

    /** coreの描画型に依存しない現在位置。 */
    default CombatantPosition position() {
        return CombatantPosition.ORIGIN;
    }
}
