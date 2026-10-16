package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;

/**
 * 外部Abilityへ渡す、core内部型を含まない実行コンテキスト。
 * @param user Abilityの使用者
 * @param target 現在の対象。存在しない場合は{@code null}
 * @param actions 所属戦闘だけを操作できる境界
 */
public record AbilityActivationContext(CombatantView user, CombatantView target, CombatActions actions) {

    public AbilityActivationContext {
        if (user == null || actions == null) {
            throw new IllegalArgumentException("Ability activation requires a user and actions");
        }
    }
}
