package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.registry.AbilityPresentation;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * API Passive RuntimeのFactory、検証、表示を結ぶ登録要素。
 */
public record PassiveImpl(String id, AbilityFactory<? extends PassiveRuntime> factory, Consumer<ConfigView> validator,
                          Function<ConfigView, AbilityPresentation> presentation) {
}
