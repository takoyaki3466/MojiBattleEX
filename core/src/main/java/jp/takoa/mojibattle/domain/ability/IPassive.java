package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;

public interface IPassive {
    String id();
    float attackMultiplier(CharacterRuntime owner, float battleElapsedSeconds, int alliedSummons);
    default float moveSpeedMultiplier(CharacterRuntime owner, float battleElapsedSeconds) { return 1f; }
    default float skillCooldownMultiplier(CharacterRuntime owner) { return 1f; }
    default float ultimateGainMultiplier(CharacterRuntime owner) { return 1f; }
    default float incomingDamageMultiplier(CharacterRuntime owner) { return 1f; }
    default float hitboxMultiplier(CharacterRuntime owner) { return 1f; }
    default float stunMultiplier(CharacterRuntime owner) { return 1f; }
    default void update(BattleController battle,CharacterRuntime owner, float delta) {}
}
