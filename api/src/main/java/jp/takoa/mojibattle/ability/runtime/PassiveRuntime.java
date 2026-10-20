package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.context.DamageEventContext;
import jp.takoa.mojibattle.ability.context.PassiveUpdateContext;
import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.modifier.NormalAttackModifier;

import java.util.List;

/**
 * メインキャラクターの常時補正と戦闘イベント反応を実装するRuntime契約。
 * 各インスタンスは一人の所有者だけに属し、別の戦闘個体と可変状態を共有してはならない。
 *
 * <h2>関係</h2>
 * <p>読取りには{@link CombatantView}、周期更新には{@link PassiveUpdateContext}、発動通知には
 * {@link AbilityActivationContext}、ダメージ通知には{@link DamageEventContext}を使用する。
 * 通常攻撃は{@link NormalAttackModifier}、HUD表示は{@link AbilityStateLine}として宣言する。
 * パッシブ登録が保持する{@link AbilityFactory}から生成され、ホストの戦闘計算と更新ループから参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * PassiveRuntime passive = new PassiveRuntime() {
 *     public float attackMultiplier(CombatantView owner) { return owner.hp() < 50f ? 1.5f : 1f; }
 *     public void onDamageDealt(DamageEventContext context) {
 *         context.actions().heal(context.source(), context.dealtDamage() * 0.1f);
 *     }
 * };
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>単純な倍率と状態を持つイベント駆動効果を同じ拡張点で表し、未使用機能はdefault実装で省略できるようにするために作成された。</p>
 */
public interface PassiveRuntime {
    default float attackMultiplier(CombatantView owner) { return 1f; }
    default float moveSpeedMultiplier(CombatantView owner) { return 1f; }
    default float skillCooldownMultiplier(CombatantView owner) { return 1f; }
    default float ultimateGainMultiplier(CombatantView owner) { return 1f; }
    default float incomingDamageMultiplier(CombatantView owner) { return 1f; }
    default float hitboxMultiplier(CombatantView owner) { return 1f; }
    default float stunMultiplier(CombatantView owner) { return 1f; }
    default long additionalUltimateAttackCount(CombatantView owner) { return 0L; }
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
