package jp.takoa.mojibattle.battle;

import java.util.List;

/** メイン、補助、召喚を同じ個体単位で参照する読取り専用の戦場ビュー。 */
public interface BattlefieldView {
    BattlefieldView EMPTY = new BattlefieldView() {
        @Override public List<CombatantView> livingCombatants() { return List.of(); }
        @Override public List<CombatantView> alliesOf(CombatantView combatant) { return List.of(); }
        @Override public List<CombatantView> enemiesOf(CombatantView combatant) { return List.of(); }
    };

    List<CombatantView> livingCombatants();
    List<CombatantView> alliesOf(CombatantView combatant);
    List<CombatantView> enemiesOf(CombatantView combatant);
}
