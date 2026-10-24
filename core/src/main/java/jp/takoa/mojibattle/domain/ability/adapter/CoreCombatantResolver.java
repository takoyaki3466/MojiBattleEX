package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.domain.BattleController;

/**
 * 公開戦闘個体ビューを、現在の戦闘で有効なCore個体へ解決する。
 */
final class CoreCombatantResolver {
    private CoreCombatantResolver() {
    }

    static Object find(BattleController battle, CombatantView view) {
        Object combatant = combatantOf(battle, view);
        return battle.isActiveBattleCombatant(combatant) ? combatant : null;
    }

    static Object requirePresent(BattleController battle, CombatantView view) {
        Object combatant = combatantOf(battle, view);
        if (!battle.isResolvableBattleCombatant(combatant)) {
            throw new IllegalArgumentException("Combatant does not belong to this battle or is no longer present");
        }
        return combatant;
    }

    private static Object combatantOf(BattleController battle, CombatantView view) {
        Object combatant = null;
        if (view instanceof CoreUnifiedCombatantView unified && unified.battle() == battle) {
            combatant = unified.combatant();
        } else if (view instanceof CoreCombatantView main) {
            combatant = main.runtime();
        }
        return combatant;
    }

    static Object require(BattleController battle, CombatantView view) {
        Object combatant = find(battle, view);
        if (combatant == null) {
            throw new IllegalArgumentException("Combatant does not belong to this battle or is no longer active");
        }
        return combatant;
    }
}
