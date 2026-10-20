package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.context.AbilityActivationContext;

/**
 * メインキャラクターのスキルを実装するRuntime契約。
 *
 * <h2>関係</h2>
 * <p>{@link AbilityActivationContext}に依存し、スキル登録が保持する{@link AbilityFactory}から個体ごとに生成される。
 * ホストは入力、クールダウン、対象選択を確認した後に{@link #tryActivate(AbilityActivationContext)}を呼ぶ。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SkillRuntime skill = context -> {
 *     if (context.target() == null) return false;
 *     context.actions().damage(context.user(), context.target(), 15f);
 *     return true;
 * };
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>発動が成立したかを戻り値でホストへ通知し、失敗時にクールダウンなどの消費を行わない判断を可能にする。</p>
 */
public interface SkillRuntime {
    boolean tryActivate(AbilityActivationContext context);

    default void activateFromUltimate(AbilityActivationContext context) {
        tryActivate(context);
    }
}
