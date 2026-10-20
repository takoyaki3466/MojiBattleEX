package jp.takoa.mojibattle.ability.config;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 設定項目の定義とConfig生成処理を管理する。
 *
 * @param <C> 生成するConfig型
 */
public final class ConfigSchema<C> {
    private final List<ConfigKey<?>> keys;
    private final Function<Values, C> factory;

    private ConfigSchema(List<ConfigKey<?>> keys, Function<Values, C> factory) {
        this.keys = List.copyOf(keys);
        this.factory = Objects.requireNonNull(factory);
    }

    /**
     * 設定項目を読み込み、Configを生成する。
     */
    public C parse(ConfigView source) {
        Map<ConfigKey<?>, Object> parsed = new HashMap<>();

        for (ConfigKey<?> key : keys) {
            parsed.put(key, key.read(source));
        }

        return factory.apply(new Values(parsed));
    }

    public static <C> Builder<C> builder() {
        return new Builder<>();
    }

    /**
     * 読み込み済み設定値。
     */
    public static final class Values {

        private final Map<ConfigKey<?>, Object> values;

        private Values(Map<ConfigKey<?>, Object> values) {
            this.values = Map.copyOf(values);
        }

        public <T> T get(ConfigKey<T> key) {
            if (!values.containsKey(key)) {
                throw new IllegalArgumentException("Unregistered config key: " + key.name());
            }

            @SuppressWarnings("unchecked") T value = (T) values.get(key);

            return value;
        }
    }

    /**
     * ConfigSchemaを構築する。
     */
    public static final class Builder<C> {

        private final List<ConfigKey<?>> keys = new ArrayList<>();

        public Builder<C> add(ConfigKey<?> key) {
            Objects.requireNonNull(key);

            if (keys.stream().anyMatch(existing -> existing.name().equals(key.name()))) {
                throw new IllegalArgumentException("Duplicate config key: " + key.name());
            }

            keys.add(key);
            return this;
        }

        public ConfigSchema<C> build(Function<Values, C> factory) {
            return new ConfigSchema<>(keys, factory);
        }
    }
}
