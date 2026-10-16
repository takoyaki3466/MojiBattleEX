package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;
import java.util.Objects;

/**
 * HUDへ渡す翻訳可能な状態行。文字列の組み立てと配置はhostが担当する。
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
