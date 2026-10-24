package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.context.AbilityActivationContext;

/**
 * メインキャラクターの必殺技を実装するRuntime契約。
 *
 * <h2>関係</h2>
 * <p>{@link AbilityActivationContext}を入力とし、必殺技登録の{@link AbilityFactory}から個体ごとに生成される。
 * ホストは必殺ゲージと発動条件を確認してから呼び出し、成功時にはパッシブ通知や既定カットイン処理を続行する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * UltimateRuntime ultimate = context -> {
 *     context.actions().enemiesOf(context.user()).forEach(enemy ->
 *             context.actions().damage(context.user(), enemy, 50f));
 *     return true;
 * };
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>スキルとは登録種別とゲージ消費を分離しつつ、同じ安全な発動コンテキストを再利用するために作成された。</p>
 */
@FunctionalInterface
public interface UltimateRuntime {
    boolean tryActivate(AbilityActivationContext context);
}
