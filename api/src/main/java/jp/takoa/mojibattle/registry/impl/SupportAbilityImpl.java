package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;

import java.util.function.Consumer;

/**
 * API Support RuntimeのFactoryと検証を結ぶ登録要素。
 */
public record SupportAbilityImpl(String id, AbilityFactory<? extends SupportAbilityRuntime> factory,
                                 Consumer<ConfigView> validator) {
}
