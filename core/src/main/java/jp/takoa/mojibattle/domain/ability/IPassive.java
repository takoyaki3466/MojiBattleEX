package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.battle.NormalAttackModifier;

import java.util.List;

/**
 * 戦闘計算の各段階へ倍率または継続効果を提供するメインキャラクターのパッシブ契約。
 */
public interface IPassive extends IAbility {
    float attackMultiplier(CharacterRuntime owner, float battleElapsedSeconds, int alliedSummons);

    default float moveSpeedMultiplier(CharacterRuntime owner, float battleElapsedSeconds) {
        return 1f;
    }

    default float skillCooldownMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    default float ultimateGainMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    default float incomingDamageMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    default float hitboxMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    default float stunMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    default float ultimateDamageMultiplier(CharacterRuntime owner) {
        return 1f;
    }

    /** 会心率へ加算する割合。0.05は5パーセントポイント。 */
    default float criticalRateModifier(CharacterRuntime owner) {
        return 0f;
    }

    /** trueの間、所有者が発生させるすべての攻撃で防御倍率を無視する。 */
    default boolean ignoresDefense(CharacterRuntime owner) {
        return false;
    }

    default List<NormalAttackModifier> normalAttackModifiers(CharacterRuntime owner) {
        return List.of();
    }

    default List<AbilityStateLine> stateLines(CharacterRuntime owner) {
        return List.of();
    }

    default void onDamageDealt(BattleController battle, CharacterRuntime source,
                               CharacterRuntime target, float amount) {
    }

    default void onDamageReceived(BattleController battle, CharacterRuntime source,
                                  CharacterRuntime target, float amount) {
    }

    /** 所有者のスキルが成功した直後に1回だけ呼ばれる。 */
    default void onSkillActivated(BattleController battle, CharacterRuntime owner) {
    }

    /** 所有者の必殺技が成功した直後に1回だけ呼ばれる。 */
    default void onUltimateActivated(BattleController battle, CharacterRuntime owner) {
    }

    /**
     * 時間経過で状態を変えるパッシブが処理を追加するための拡張点。
     */
    default void update(BattleController battle, CharacterRuntime owner, float delta) {
    }
}
