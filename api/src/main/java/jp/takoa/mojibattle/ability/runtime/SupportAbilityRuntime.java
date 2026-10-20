package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;

/**
 * 補助キャラクターのスキルと必殺技に共通する実行契約。
 *
 * <h2>関係</h2>
 * <p>{@link SupportAbilityActivationContext}に依存し、補助スキル／必殺技の登録が保持する
 * {@link AbilityFactory}から補助個体ごとに生成される。ホストの補助コントローラーから依存される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SupportAbilityRuntime runtime = context -> {
 *     context.actions().heal(context.owner(), 10f);
 *     return true;
 * };
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>補助スキルと補助必殺技は入力形状が同じため、重複インターフェースを増やさず登録種別側で意味を区別する。</p>
 */
@FunctionalInterface
public interface SupportAbilityRuntime {
    boolean activate(SupportAbilityActivationContext context);
}
