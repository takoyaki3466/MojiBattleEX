package jp.takoa.mojibattle.battle.combatant;

/**
 * MODへ公開する、一体の戦闘参加者の読取り専用ビュー。
 *
 * <h2>関係</h2>
 * <p>種別を{@link CombatantKind}、位置を{@link CombatantPosition}で返す。
 * {@link BattlefieldView}、Abilityの各context、および
 * {@link jp.takoa.mojibattle.battle.action.CombatActions}のほぼすべての操作から参照される。
 * ホスト側のメイン・補助・召喚アダプターが実装する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * if (target.alive() && target.hp() / target.maximumHp() < 0.25f) {
 *     actions.damage(user, target, 20f);
 * }
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>coreの可変エンティティをAPIから隔離し、識別・現在値・位置だけを安定した契約として公開するために作成された。</p>
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
