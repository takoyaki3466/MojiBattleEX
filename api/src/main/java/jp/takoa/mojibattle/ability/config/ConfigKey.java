package jp.takoa.mojibattle.ability.config;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 型付き設定項目。
 *
 * @param <T> 設定値の型
 */
public record ConfigKey<T>(String name, Function<ConfigView, T> reader) {

    public ConfigKey {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Config key is empty");
        }

        Objects.requireNonNull(reader);
    }

    /**
     * 設定値を取得し、検証する。
     */
    public T read(ConfigView values) {
        return reader.apply(values);
    }

    public static ConfigKey<Float> nonNegativeFloat(String name, float defaultValue) {
        if (!Float.isFinite(defaultValue) || defaultValue < 0f) {
            throw new IllegalArgumentException("Default value must be finite and non-negative: " + name);
        }
        return new ConfigKey<>(name, values -> ConfigValues.nonNegativeFloat(values, name, defaultValue));
    }

    public static ConfigKey<Float> positiveFloat(String name, float defaultValue) {
        if (!Float.isFinite(defaultValue) || defaultValue <= 0f) {
            throw new IllegalArgumentException("Default value must be finite and positive: " + name);
        }
        return new ConfigKey<>(name, values -> ConfigValues.positiveFloat(values, name, defaultValue));
    }

    public static ConfigKey<Integer> positiveInt(String name, int defaultValue) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("Default value must be positive: " + name);
        }
        return new ConfigKey<>(name, values -> ConfigValues.positiveInt(values, name, defaultValue));
    }

    public static ConfigKey<ResourceId> resourceId(String name, String defaultValue) {
        ResourceId.parse(Objects.requireNonNull(defaultValue, "Default resource ID is required"));
        return new ConfigKey<>(name, values -> ConfigValues.resourceId(values, name, defaultValue));
    }

    public static ConfigKey<Boolean> booleanValue(String name, boolean defaultValue) {
        return new ConfigKey<>(name, values -> ConfigValues.booleanValue(values, name, defaultValue));
    }

    public static ConfigKey<String> stringValue(String name, String defaultValue) {
        Objects.requireNonNull(defaultValue, "Default string is required");
        return new ConfigKey<>(name, values -> ConfigValues.stringValue(values, name, defaultValue));
    }

    public ConfigKey<T> validate(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "Predicate is required");
        return new ConfigKey<>(name, values -> {
            T value = read(values);

            if (!predicate.test(value)) {
                throw new IllegalArgumentException("Invalid config value: " + name);
            }

            return value;
        });
    }
}
