package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;

/**
 * Ability設定から、戦闘個体専用のRuntimeインスタンスを生成するファクトリー。
 *
 * <h2>関係</h2>
 * <p>{@link ConfigView}だけに依存する。スキル、必殺技、パッシブなどの登録実装がこのファクトリーを保持し、
 * ホストが装備ロード時に{@link #create(ConfigView)}を呼び出す。生成対象は通常、
 * {@link SkillRuntime}、{@link UltimateRuntime}、{@link PassiveRuntime}、
 * {@link SupportAbilityRuntime}または{@link SupportPassiveRuntime}である。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * AbilityFactory<SkillRuntime> factory = config ->
 *         context -> context.actions().damage(context.user(), context.target(),
 *                 config.floatValue("damage", 10f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Runtimeの可変状態をキャラクター間で共有しないため、登録値そのものではなく生成処理を保持する。</p>
 *
 * @param <T> 生成するRuntime型
 */
@FunctionalInterface
public interface AbilityFactory<T> {
    /**
     * @param parameters JSONの{@code parameters}。未指定時は空View
     * @return 新しい装備者専用Runtime。{@code null}不可
     */
    T create(ConfigView parameters);
}
