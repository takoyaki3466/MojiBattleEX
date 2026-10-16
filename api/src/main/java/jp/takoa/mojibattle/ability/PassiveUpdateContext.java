package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;

import java.util.Objects;

/**
 * 戦闘の固定ステップごとにパッシブへ渡される不変コンテキスト。
 * ポーズ中および勝敗確定後は呼び出されない。
 * @param owner このRuntimeを所有する参加者
 * @param actions 所属戦闘だけを操作できる境界
 * @param deltaSeconds 今回の固定ステップ秒
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record PassiveUpdateContext(CombatantView owner, CombatActions actions, float deltaSeconds,
                                   float battleElapsedSeconds) {
    public PassiveUpdateContext {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
    }
}
