package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.battle.combatant.BattlefieldView;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.domain.BattleController;

import java.util.List;

/** 現在の全戦闘個体を個体ID単位で列挙するビュー。 */
public final class CoreBattlefieldView implements BattlefieldView {
    private final BattleController battle;
    public CoreBattlefieldView(BattleController battle) { this.battle = battle; }

    @Override public List<CombatantView> livingCombatants() {
        return battle.livingBattleCombatants().stream().map(value -> (CombatantView) view(value)).toList();
    }
    @Override public List<CombatantView> alliesOf(CombatantView combatant) {
        Object source = resolve(combatant);
        return battle.alliedBattleCombatants(source).stream().map(value -> (CombatantView) view(value)).toList();
    }
    @Override public List<CombatantView> enemiesOf(CombatantView combatant) {
        Object source = resolve(combatant);
        return battle.enemyBattleCombatants(source).stream().map(value -> (CombatantView) view(value)).toList();
    }
    public CoreUnifiedCombatantView view(Object value) { return new CoreUnifiedCombatantView(battle, value); }
    Object resolve(CombatantView value) {
        return CoreCombatantResolver.require(battle, value);
    }
}
