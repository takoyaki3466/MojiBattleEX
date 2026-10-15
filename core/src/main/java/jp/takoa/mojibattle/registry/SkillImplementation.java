package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.ISkill;

import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * スキル設定の生成、固有検証、UI表示を同じ実装IDへ結び付ける登録要素。
 */
public record SkillImplementation(String id, Function<AbilityConfigs.ProjectileSkill, ISkill> factory,
                                  Consumer<AbilityConfigs.ProjectileSkill> validator,
                                  BiFunction<AbilityConfigs.ProjectileSkill, Float, AbilityPresentation> presentation) {
}
