package jp.takoa.mojibattle.ability.config;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.resource.ResourceId;

/**
 * ConfigViewから基本型を厳密に読み取り、既定値を含めて検証する。
 */
public final class ConfigValues {
    private ConfigValues() {
    }

    public static float positiveFloat(ConfigView config, String key, float fallback) {
        float value = floatValue(config, key, fallback);
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(key + " must be finite and positive");
        }
        return value;
    }

    public static float nonNegativeFloat(ConfigView config, String key, float fallback) {
        float value = floatValue(config, key, fallback);
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(key + " must be finite and non-negative");
        }
        return value;
    }

    public static int positiveInt(ConfigView config, String key, int fallback) {
        int value = intValue(config, key, fallback);
        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return value;
    }

    public static ResourceId resourceId(ConfigView config, String key, String fallback) {
        return ResourceId.parse(stringValue(config, key, fallback));
    }

    public static boolean booleanValue(ConfigView config, String key, boolean fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        Object raw = config.value(key);
        if (!(raw instanceof Boolean value)) {
            throw typeError(key, "boolean", raw);
        }
        return value;
    }

    public static String stringValue(ConfigView config, String key, String fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        Object raw = config.value(key);
        if (!(raw instanceof String value)) {
            throw typeError(key, "string", raw);
        }
        return value;
    }

    private static float floatValue(ConfigView config, String key, float fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        Object raw = config.value(key);
        if (!(raw instanceof Number number)) {
            throw typeError(key, "number", raw);
        }
        return number.floatValue();
    }

    private static int intValue(ConfigView config, String key, int fallback) {
        if (!config.contains(key)) {
            return fallback;
        }
        Object raw = config.value(key);
        if (!(raw instanceof Number number)) {
            throw typeError(key, "integer", raw);
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " must be an exact int");
        }
        return (int) value;
    }

    private static IllegalArgumentException typeError(String key, String expected, Object raw) {
        return new IllegalArgumentException(key + " must be " + expected + ", but was " + (raw == null ? "null" : raw.getClass().getName()));
    }
}
