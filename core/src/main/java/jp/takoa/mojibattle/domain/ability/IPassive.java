package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;

/** 戦闘計算の各段階へ倍率または継続効果を提供するメインキャラクターのパッシブ契約。 */
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

    /** 時間経過で状態を変えるパッシブが処理を追加するための拡張点。 */
    default void update(BattleController battle, CharacterRuntime owner, float delta) {
    }
}
