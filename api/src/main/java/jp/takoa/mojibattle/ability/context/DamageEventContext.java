package jp.takoa.mojibattle.ability.context;

import jp.takoa.mojibattle.battle.action.CombatActions;
import jp.takoa.mojibattle.battle.combatant.CombatantView;

import java.util.Objects;

/**
 * 確定ダメージ後に、攻撃側・被弾側パッシブへ通知する読取りコンテキスト。
 *
 * <h2>関係</h2>
 * <p>{@link CombatantView}で発生元と対象を表し、追加効果は{@link CombatActions}へ要求する。
 * {@link jp.takoa.mojibattle.ability.runtime.PassiveRuntime#onDamageDealt(DamageEventContext)}と
 * {@link jp.takoa.mojibattle.ability.runtime.PassiveRuntime#onDamageReceived(DamageEventContext)}から利用される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * if (context.dealtDamage() >= 50f) {
 *     context.actions().heal(context.source(), 5f);
 * }
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>要求ダメージではなく実際にHPへ反映された値を通知し、会心・防御・残HPの差異をパッシブ側で再計算させないために作成された。</p>
 *
 * @param source               ダメージの発生元
 * @param target               ダメージを受けた参加者
 * @param dealtDamage          HPへ実際に適用された非負ダメージ
 * @param actions              同じ戦闘に対する操作境界
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record DamageEventContext(CombatantView source, CombatantView target, float dealtDamage, CombatActions actions, float battleElapsedSeconds) {
    public DamageEventContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(actions, "actions");
        if (!Float.isFinite(dealtDamage) || dealtDamage < 0) {
            throw new IllegalArgumentException("dealtDamage must be finite and non-negative");
        }
    }
}
