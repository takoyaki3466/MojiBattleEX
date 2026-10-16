package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.NormalAttackModifier;

import java.util.List;

/** プレイヤーごとに生成され、所有者以外と可変状態を共有しない外部パッシブ実装。 */
public interface PassiveRuntime {
    /** 固定ステップごとに1回呼ばれる。ポーズ中と戦闘終了後は呼ばれない。 */
    default void update(PassiveUpdateContext context) {
    }

    default void onSkillActivated(AbilityActivationContext context) {
    }

    default void onUltimateActivated(AbilityActivationContext context) {
    }

    /** 必殺技ダメージを確定するたびに評価される。 */
    default float ultimateDamageMultiplier(CombatantView owner) {
        return 1f;
    }

    /**
     * 会心判定ごとに再評価される加算補正。{@code 0.05f}は5パーセントポイントを表す。
     * 複数パッシブの値は加算され、最終率は0から1へクランプされる。
     */
    default float criticalRateModifier(CombatantView owner) {
        return 0f;
    }

    /** trueの間、所有者が発生させる全攻撃は防御倍率を無視する。 */
    default boolean ignoresDefense(CombatantView owner) {
        return false;
    }

    /** 通常攻撃を行うたびに再評価される補正。 */
    default List<NormalAttackModifier> normalAttackModifiers(CombatantView owner) {
        return List.of();
    }

    /** HUD更新時に取得され、priority降順・翻訳キー順で最大8件表示される。 */
    default List<AbilityStateLine> stateLines(CombatantView owner) {
        return List.of();
    }

    default void onDamageDealt(DamageEventContext context) {
    }

    default void onDamageReceived(DamageEventContext context) {
    }
}
