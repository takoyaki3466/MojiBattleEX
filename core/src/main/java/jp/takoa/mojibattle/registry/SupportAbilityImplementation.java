package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.support.SupportAbilityContext;
import jp.takoa.mojibattle.domain.support.SupportAbilityDefinition;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 補助スキル／必殺の実行ハンドラと固有検証を登録する要素。
 */
public record SupportAbilityImplementation(String id, BiConsumer<SupportAbilityContext, Boolean> handler, Consumer<SupportAbilityDefinition> validator) {
}
