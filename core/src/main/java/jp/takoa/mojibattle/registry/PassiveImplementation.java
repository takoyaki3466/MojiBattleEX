package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.IPassive;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * パッシブ設定の生成、固有検証、UI表示を同じ実装IDへ結び付ける登録要素。
 */
public record PassiveImplementation(String id, Function<AbilityConfigs.TimedPassive, IPassive> factory,
                                    Consumer<AbilityConfigs.TimedPassive> validator,
                                    Function<AbilityConfigs.TimedPassive, AbilityPresentation> presentation) {
}
