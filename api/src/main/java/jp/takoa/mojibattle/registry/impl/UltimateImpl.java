package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.UltimateRuntime;
import jp.takoa.mojibattle.registry.AbilityPresentation;

import java.util.function.Consumer;
import java.util.function.Function;

/** API Ultimate RuntimeのFactory、検証、表示を結ぶ登録要素。 */
public record UltimateImpl(String id, AbilityFactory<? extends UltimateRuntime> factory,
                           Consumer<ConfigView> validator,
                           Function<ConfigView, AbilityPresentation> presentation) { }
