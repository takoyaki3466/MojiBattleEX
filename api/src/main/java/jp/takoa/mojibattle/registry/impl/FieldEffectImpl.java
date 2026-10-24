package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;

import java.util.function.Consumer;

/**
 * フィールド効果typeと公開設定検証を結ぶ登録要素。
 */
public record FieldEffectImpl(String id, Consumer<ConfigView> validator) {
}
