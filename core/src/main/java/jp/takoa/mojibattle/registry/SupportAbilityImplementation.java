package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.support.SupportAbilityContext;
import jp.takoa.mojibattle.domain.support.SupportAbilityDefinition;

import java.util.function.Consumer;
import java.util.function.BiFunction;

/**
 * 補助スキル／必殺の実行ハンドラと固有検証を登録する要素。
 */
public record SupportAbilityImplementation(String id, BiFunction<SupportAbilityContext, Boolean, Boolean> handler,
                                           Consumer<SupportAbilityDefinition> validator) {
}
