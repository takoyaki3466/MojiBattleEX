package jp.takoa.mojibattle.battle.combatant;

import java.util.List;

/**
 * メイン、補助、召喚を同じ個体単位で検索する読取り専用の戦場ビュー。
 *
 * <h2>関係</h2>
 * <p>要素型の{@link CombatantView}に依存し、{@link jp.takoa.mojibattle.battle.action.CombatActions#battlefield()}から提供される。
 * ホストの戦場アダプターが実装し、Ability Runtimeが味方・敵・全生存個体の照会に使用する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * long summons = actions.battlefield().alliesOf(user).stream()
 *         .filter(unit -> unit.kind() == CombatantKind.SUMMON)
 *         .count();
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Abilityがcoreの参加者コレクションを直接変更しないよう、スナップショット的な検索境界として作成された。</p>
 */
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
