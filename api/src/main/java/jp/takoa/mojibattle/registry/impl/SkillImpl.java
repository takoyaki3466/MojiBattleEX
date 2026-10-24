package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.registry.AbilityPresentation;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * API Skill RuntimeのFactory、検証、表示を結ぶ登録要素。
 */
public record SkillImpl(String id, AbilityFactory<? extends SkillRuntime> factory, Consumer<ConfigView> validator,
                        BiFunction<ConfigView, Float, AbilityPresentation> presentation) {
}
