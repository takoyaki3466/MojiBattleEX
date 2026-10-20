package jp.takoa.mojibattle.ability.config;

import java.util.Objects;

/**
 * 型付きConfigを保持するRuntimeの任意基底クラス。
 * Runtime契約の実装に継承は必須ではなく、定型的な保持処理だけを提供する。
 *
 * @param <C> Config型
 */
public abstract class AbstractRuntime<C> {
    private final C config;

    /** Runtime個体専用のConfigを保持する。 */
    public AbstractRuntime(C config) {
        this.config = Objects.requireNonNull(config, "Config is required");
    }

    public C getConfig() {
        return config;
    }
}
