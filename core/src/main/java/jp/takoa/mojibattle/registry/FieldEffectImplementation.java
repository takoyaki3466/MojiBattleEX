package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.config.FieldEffectDefinition;

import java.util.function.Consumer;

/**
 * フィールド効果typeの登録と固有検証を結び付ける。
 */
public record FieldEffectImplementation(String id, Consumer<FieldEffectDefinition> validator) {
}
