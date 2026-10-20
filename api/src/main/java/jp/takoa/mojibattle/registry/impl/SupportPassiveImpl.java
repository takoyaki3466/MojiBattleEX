package jp.takoa.mojibattle.registry.impl;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.runtime.AbilityFactory;
import jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime;

import java.util.Objects;
import java.util.function.Consumer;

/** API Support Passive RuntimeのFactoryと設定検証を結ぶ登録要素。 */
public record SupportPassiveImpl(String id,
                                 AbilityFactory<? extends SupportPassiveRuntime> factory,
                                 Consumer<ConfigView> validator) {
    public SupportPassiveImpl {
        Objects.requireNonNull(id, "Support passive ID is required");
        Objects.requireNonNull(factory, "Support passive factory is required");
        Objects.requireNonNull(validator, "Support passive validator is required");
    }

    /** 従来の詳細登録との互換性を保つ、追加検証なしのコンストラクタ。 */
    public SupportPassiveImpl(String id, AbilityFactory<? extends SupportPassiveRuntime> factory) {
        this(id, factory, ignored -> { });
    }
}
