package jp.takoa.mojibattle.ability.presentation;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;
import java.util.Objects;

/**
 * AbilityからHUDへ渡す、翻訳可能な一行の状態表示。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.runtime.PassiveRuntime}と
 * {@link jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime}が生成し、
 * {@link jp.takoa.mojibattle.ability.support.SupportActions#setStateLines(List)}経由でも設定できる。
 * ホストのHUDが翻訳、優先度による並べ替え、およびアイコン描画を担当する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * AbilityStateLine line = AbilityStateLine.of("mod.ability.remaining", 100, remainingSeconds);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Runtimeへフォント・色・座標などの描画依存を持ち込まず、ローカライズ可能な意味データだけを受け渡すために作成された。</p>
 *
 * @param translationKey 翻訳辞書キー
 * @param arguments 翻訳へ渡す不変引数列
 * @param priority 大きい値を先に表示する優先順位
 * @param iconId 任意の名前空間付きアイコン。未指定時は{@code null}
 */
public record AbilityStateLine(String translationKey, List<Object> arguments, int priority,
                               ResourceId iconId) {
    public AbilityStateLine {
        if (translationKey == null || translationKey.isBlank()) {
            throw new IllegalArgumentException("translationKey is required");
        }
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }

    public static AbilityStateLine of(String translationKey, int priority, Object... arguments) {
        return new AbilityStateLine(translationKey, List.of(arguments), priority, null);
    }
}
