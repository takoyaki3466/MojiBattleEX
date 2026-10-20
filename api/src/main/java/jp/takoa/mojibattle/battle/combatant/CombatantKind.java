package jp.takoa.mojibattle.battle.combatant;

/**
 * 戦場に存在する戦闘個体の役割種別。
 *
 * <h2>関係</h2>
 * <p>{@link CombatantView#kind()}が返し、{@link BattlefieldView}から得た混合集合をAbilityが分類するために使用する。
 * ホストのメイン、補助、召喚アダプターが対応する値を選ぶ。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * boolean temporary = combatant.kind() == CombatantKind.SUMMON;
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>{@code instanceof}やcoreクラスへの依存なしに、全戦闘個体を同じビューで扱えるようにするために作成された。</p>
 */
public enum CombatantKind {
    MAIN,
    SUPPORT,
    SUMMON
}
