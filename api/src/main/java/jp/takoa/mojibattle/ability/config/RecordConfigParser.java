package jp.takoa.mojibattle.ability.config;

import jp.takoa.mojibattle.ability.config.annotations.*;
import jp.takoa.mojibattle.resource.ResourceId;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * recordの設定宣言を一度解析し、宣言順を保ったまま全項目を読み取る。
 *
 * @param <C> 生成する設定record型
 */
public final class RecordConfigParser<C> {
    private final Constructor<C> constructor;
    private final List<ComponentBinding> bindings;
    private final List<Integer> displayIndexes;

    /**
     * 設定recordの構造、アノテーション、既定値を解析する。
     *
     * @param configType 設定record型
     * @throws IllegalArgumentException 宣言が曖昧または未対応の場合
     */
    public RecordConfigParser(Class<C> configType) {
        Objects.requireNonNull(configType, "Config type is required");
        if (!configType.isRecord()) {
            throw new IllegalArgumentException("Config must be a record: " + configType.getName());
        }
        if (!Modifier.isPublic(configType.getModifiers())) {
            throw new IllegalArgumentException("Config record must be public: " + configType.getName());
        }

        RecordComponent[] components = configType.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        ArrayList<ComponentBinding> parsedBindings = new ArrayList<>(components.length);
        ArrayList<Integer> parsedDisplayIndexes = new ArrayList<>();
        Set<String> keys = new HashSet<>();

        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            parameterTypes[index] = component.getType();
            ComponentBinding binding = binding(component);
            if (!keys.add(binding.key())) {
                throw definitionError(component, "Duplicate config key: " + binding.key());
            }
            parsedBindings.add(binding);
            if (component.isAnnotationPresent(DisplayValue.class)) {
                parsedDisplayIndexes.add(index);
            }
        }

        try {
            constructor = configType.getConstructor(parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException(
                    "Config canonical constructor must be public: " + configType.getName(), exception);
        }
        bindings = List.copyOf(parsedBindings);
        displayIndexes = List.copyOf(parsedDisplayIndexes);
    }

    /** 全項目を解析・検証して独立した設定recordを生成する。 */
    public C parse(ConfigView source) {
        Objects.requireNonNull(source, "Config source is required");
        Object[] values = new Object[bindings.size()];
        for (int index = 0; index < bindings.size(); index++) {
            values[index] = bindings.get(index).read(source);
        }
        try {
            return constructor.newInstance(values);
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new IllegalStateException("Unable to create config: " + constructor.getDeclaringClass().getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalArgumentException("Config constructor rejected values: "
                    + constructor.getDeclaringClass().getName(), cause);
        }
    }

    /**
     * 解析済み設定から、{@link DisplayValue}の宣言順で表示引数を取得する。
     * 数値には指定倍率を適用し、それ以外の値はそのまま返す。
     */
    public Object[] displayValues(C config, float numericMultiplier) {
        Objects.requireNonNull(config, "Config is required");
        if (!Float.isFinite(numericMultiplier)) {
            throw new IllegalArgumentException("Display multiplier must be finite");
        }
        Object[] values = new Object[displayIndexes.size()];
        for (int index = 0; index < displayIndexes.size(); index++) {
            Object value = bindings.get(displayIndexes.get(index)).access(config);
            values[index] = value instanceof Number number
                    ? number.floatValue() * numericMultiplier
                    : value;
        }
        return values;
    }

    /** 設定ビューを検証済みrecordへ変換してから表示引数を取得する。 */
    public Object[] displayValues(ConfigView source, float numericMultiplier) {
        return displayValues(parse(source), numericMultiplier);
    }

    private static ComponentBinding binding(RecordComponent component) {
        ArrayList<Annotation> configAnnotations = new ArrayList<>(1);
        add(configAnnotations, component.getAnnotation(ConfigFloat.class));
        add(configAnnotations, component.getAnnotation(ConfigInt.class));
        add(configAnnotations, component.getAnnotation(ConfigBoolean.class));
        add(configAnnotations, component.getAnnotation(ConfigString.class));
        add(configAnnotations, component.getAnnotation(ConfigResource.class));
        add(configAnnotations, component.getAnnotation(ConfigList.class));
        if (configAnnotations.size() != 1) {
            throw definitionError(component, "Exactly one supported config annotation is required");
        }

        Annotation annotation = configAnnotations.getFirst();
        String key;
        Function<Object, Object> converter;
        Object defaultValue;
        if (annotation instanceof ConfigFloat rule) {
            requireType(component, float.class, ConfigFloat.class);
            validateFloatRule(rule, component);
            key = key(component, rule.name());
            defaultValue = rule.defaultValue();
            converter = raw -> readFloat(raw, rule, key);
        } else if (annotation instanceof ConfigInt rule) {
            requireType(component, int.class, ConfigInt.class);
            validateIntRule(rule, component);
            key = key(component, rule.name());
            defaultValue = rule.defaultValue();
            converter = raw -> readInt(raw, rule, key);
        } else if (annotation instanceof ConfigBoolean rule) {
            requireType(component, boolean.class, ConfigBoolean.class);
            key = key(component, rule.name());
            defaultValue = rule.defaultValue();
            converter = raw -> {
                if (!(raw instanceof Boolean value)) {
                    throw typeError(key, "boolean", raw);
                }
                return value;
            };
        } else if (annotation instanceof ConfigString rule) {
            requireType(component, String.class, ConfigString.class);
            key = key(component, rule.name());
            validateString(rule.defaultValue(), rule, key, "default");
            defaultValue = rule.defaultValue();
            converter = raw -> {
                if (!(raw instanceof String value)) {
                    throw typeError(key, "string", raw);
                }
                validateString(value, rule, key, "value");
                return value;
            };
        } else if (annotation instanceof ConfigResource rule) {
            requireType(component, ResourceId.class, ConfigResource.class);
            key = key(component, rule.name());
            ResourceId parsedDefault = rule.nullable() && rule.defaultValue().isBlank()
                    ? null : parseResource(rule.defaultValue(), key, "default");
            defaultValue = parsedDefault;
            converter = raw -> {
                if (raw == null && rule.nullable()) {
                    return null;
                }
                if (!(raw instanceof String value)) {
                    throw typeError(key, "resource ID string", raw);
                }
                return parseResource(value, key, "value");
            };
        } else {
            ConfigList rule = (ConfigList) annotation;
            requireListType(component, rule);
            key = key(component, rule.name());
            defaultValue = List.of();
            converter = raw -> readList(raw, rule, key, 1);
        }

        return new ComponentBinding(key, defaultValue,
                component.isAnnotationPresent(RequiredConfig.class), converter, component);
    }

    private static void validateFloatRule(ConfigFloat rule, RecordComponent component) {
        if (!Float.isFinite(rule.defaultValue()) || Float.isNaN(rule.min()) || Float.isNaN(rule.max())
                || rule.min() > rule.max()
                || ((rule.exclusiveMin() || rule.exclusiveMax()) && rule.min() == rule.max())) {
            throw definitionError(component, "Invalid float bounds or default");
        }
        checkFloatRange(rule.defaultValue(), rule, component.getName(), "default");
    }

    private static void validateIntRule(ConfigInt rule, RecordComponent component) {
        if (rule.min() > rule.max()
                || ((rule.exclusiveMin() || rule.exclusiveMax()) && rule.min() == rule.max())) {
            throw definitionError(component, "Invalid int bounds");
        }
        checkIntRange(rule.defaultValue(), rule, component.getName(), "default");
    }

    private static float readFloat(Object raw, ConfigFloat rule, String key) {
        if (!(raw instanceof Number number)) {
            throw typeError(key, "number", raw);
        }
        float value = number.floatValue();
        checkFloatRange(value, rule, key, "value");
        return value;
    }

    private static int readInt(Object raw, ConfigInt rule, String key) {
        if (!(raw instanceof Number number)) {
            throw typeError(key, "integer", raw);
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Config '" + key + "' must be an exact int, but was " + raw);
        }
        int result = (int) value;
        checkIntRange(result, rule, key, "value");
        return result;
    }

    private static void checkFloatRange(float value, ConfigFloat rule, String key, String origin) {
        if (!Float.isFinite(value)
                || (rule.exclusiveMin() ? value <= rule.min() : value < rule.min())
                || (rule.exclusiveMax() ? value >= rule.max() : value > rule.max())) {
            throw new IllegalArgumentException("Config '" + key + "' " + origin
                    + " is outside " + range(rule.min(), rule.max(), rule.exclusiveMin(), rule.exclusiveMax())
                    + ": " + value);
        }
    }

    private static void checkIntRange(int value, ConfigInt rule, String key, String origin) {
        if ((rule.exclusiveMin() ? value <= rule.min() : value < rule.min())
                || (rule.exclusiveMax() ? value >= rule.max() : value > rule.max())) {
            throw new IllegalArgumentException("Config '" + key + "' " + origin
                    + " is outside " + range(rule.min(), rule.max(), rule.exclusiveMin(), rule.exclusiveMax())
                    + ": " + value);
        }
    }

    private static void validateString(String value, ConfigString rule, String key, String origin) {
        if (!rule.allowBlank() && value.isBlank()) {
            throw new IllegalArgumentException("Config '" + key + "' " + origin + " must not be blank");
        }
    }

    private static ResourceId parseResource(String value, String key, String origin) {
        try {
            return ResourceId.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Config '" + key + "' has an invalid resource ID "
                    + origin + ": " + value, exception);
        }
    }

    private static Object readList(Object raw, ConfigList rule, String key, int dimension) {
        if (!(raw instanceof List<?> values)) {
            throw typeError(key, "list", raw);
        }
        ArrayList<Object> converted = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (dimension < rule.dimensions()) {
                converted.add(readList(value, rule, key + "[" + index + "]", dimension + 1));
            } else if (rule.value() == ConfigList.ValueType.STRING && value instanceof String text) {
                converted.add(text);
            } else if (rule.value() == ConfigList.ValueType.FLOAT && value instanceof Number number
                    && Float.isFinite(number.floatValue())) {
                converted.add(number.floatValue());
            } else {
                throw typeError(key + "[" + index + "]", rule.value().name().toLowerCase(), value);
            }
        }
        return List.copyOf(converted);
    }

    private static void requireListType(RecordComponent component, ConfigList rule) {
        if (component.getType() != List.class || rule.dimensions() < 1 || rule.dimensions() > 2) {
            throw definitionError(component, "@ConfigList requires List with one or two dimensions");
        }
        Type expectedElement = rule.value() == ConfigList.ValueType.STRING ? String.class : Float.class;
        Type actual = component.getGenericType();
        for (int dimension = 0; dimension < rule.dimensions(); dimension++) {
            if (!(actual instanceof ParameterizedType parameterized)
                    || parameterized.getRawType() != List.class
                    || parameterized.getActualTypeArguments().length != 1) {
                throw definitionError(component, "@ConfigList generic type does not match its dimensions");
            }
            actual = parameterized.getActualTypeArguments()[0];
        }
        if (actual != expectedElement) {
            throw definitionError(component, "@ConfigList element type must be " + expectedElement.getTypeName());
        }
    }

    private static void requireType(RecordComponent component, Class<?> expected,
                                    Class<? extends Annotation> annotation) {
        if (component.getType() != expected) {
            throw definitionError(component, "@" + annotation.getSimpleName()
                    + " requires " + expected.getSimpleName() + " but found " + component.getType().getTypeName());
        }
    }

    private static String key(RecordComponent component, String declaredName) {
        return declaredName.isBlank() ? component.getName() : declaredName;
    }

    private static String range(Number min, Number max, boolean exclusiveMin, boolean exclusiveMax) {
        return (exclusiveMin ? "(" : "[") + min + ", " + max + (exclusiveMax ? ")" : "]");
    }

    private static IllegalArgumentException typeError(String key, String expected, Object actual) {
        String actualType = actual == null ? "null" : actual.getClass().getName();
        return new IllegalArgumentException("Config '" + key + "' must be " + expected + ", but was " + actualType);
    }

    private static IllegalArgumentException definitionError(RecordComponent component, String message) {
        return new IllegalArgumentException(message + " at "
                + component.getDeclaringRecord().getName() + "." + component.getName());
    }

    private static void add(List<Annotation> annotations, Annotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    private record ComponentBinding(String key, Object defaultValue, boolean required,
                                    Function<Object, Object> converter, RecordComponent component) {
        private Object read(ConfigView source) {
            if (!source.contains(key)) {
                if (required) {
                    throw new IllegalArgumentException("Required config is missing: " + key);
                }
                return defaultValue;
            }
            return converter.apply(source.value(key));
        }

        private Object access(Object config) {
            try {
                return component.getAccessor().invoke(config);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read config component: " + key, exception);
            }
        }
    }
}
