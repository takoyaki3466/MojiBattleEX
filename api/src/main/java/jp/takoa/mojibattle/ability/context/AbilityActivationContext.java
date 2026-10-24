package jp.takoa.mojibattle.ability.context;

import jp.takoa.mojibattle.battle.action.CombatActions;
import jp.takoa.mojibattle.battle.combatant.CombatantView;

/**
 * メインキャラクターのスキルまたは必殺技へ渡す、一回の発動コンテキスト。
 *
 * <h2>関係</h2>
 * <p>{@link CombatantView}と{@link CombatActions}に依存し、
 * {@link jp.takoa.mojibattle.ability.runtime.SkillRuntime}、
 * {@link jp.takoa.mojibattle.ability.runtime.UltimateRuntime}および
 * {@link jp.takoa.mojibattle.ability.runtime.PassiveRuntime}から参照される。
 * ホスト側アダプターが戦闘ドメイン値をこのAPI型へ変換して生成する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * CombatantView target = context.target();
 * if (target != null) {
 *     context.actions().damage(context.user(), target, 20f);
 * }
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Ability実装がcoreの可変クラスを保持・変更することを防ぎ、許可された操作だけを明示するために作成された。</p>
 *
 * @param user    Abilityの使用者
 * @param target  現在の対象。存在しない場合は{@code null}
 * @param actions 所属戦闘だけを操作できる境界
 */
public record AbilityActivationContext(CombatantView user, CombatantView target, CombatActions actions) {

    public AbilityActivationContext {
        if (user == null || actions == null) {
            throw new IllegalArgumentException("Ability activation requires a user and actions");
        }
    }
}
