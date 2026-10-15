package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.IUltimate;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 必殺技設定の生成、固有検証、UI表示を同じ実装IDへ結び付ける登録要素。
 */
public record UltimateImplementation(String id, Function<AbilityConfigs.AreaUltimate, IUltimate> factory,
                                     Consumer<AbilityConfigs.AreaUltimate> validator,
                                     Function<AbilityConfigs.AreaUltimate, AbilityPresentation> presentation) {
}
