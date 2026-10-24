package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * JSONローダーのMap表現をAPIの不変値だけへ変換する境界。
 */
public final class MapConfigView implements ConfigView {
    private final Map<String, Object> values;

    private MapConfigView(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();

        if (source != null) {
            source.forEach((key, value) -> copy.put(key, sanitize(value)));
        }
        values = Collections.unmodifiableMap(copy);
    }

    public static ConfigView of(Map<String, Object> values) {
        return values == null || values.isEmpty() ? ConfigView.empty() : new MapConfigView(values);
    }

    /**
     * Ability DTOを旧形式またはparameters形式に応じた読取りビューへ変換する。
     *
     * <p>名前空間付き実装IDを持つ新形式では{@code parameters}だけを公開する。
     * これにより、省略した項目はConfigアノテーションの既定値へ委ねられ、
     * 明示した0などの値はそのまま検証対象になる。名前空間を持たない旧形式では、
     * 後方互換性のため従来どおりDTOのpublic fieldを公開する。</p>
     */
    public static ConfigView of(Object bean) {
        if (bean == null) {
            return ConfigView.empty();
        }

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();

        Map<?, ?> parameters = null;

        boolean hasImplementationIdField = false;

        String namespacedImplementationId = null;
        String namespacedType = null;

        try {
            for (Field field : bean.getClass().getFields()) {
                Object value = field.get(bean);

                if ("parameters".equals(field.getName()) && value instanceof Map<?, ?> map) {
                    parameters = map;

                } else if ("implementationId".equals(field.getName())) {
                    hasImplementationIdField = true;
                    if (value instanceof String id && id.contains(":")) {
                        namespacedImplementationId = id;
                    }
                } else if ("type".equals(field.getName()) && value instanceof String id && id.contains(":")) {
                    namespacedType = id;
                }

                values.put(field.getName(), value);
            }

        } catch (IllegalAccessException error) {
            throw new IllegalArgumentException("Cannot expose Ability configuration", error);
        }

        boolean parameterized = hasImplementationIdField ? namespacedImplementationId != null : namespacedType != null;

        if (parameterized) {
            if (parameters == null || parameters.isEmpty()) {
                return ConfigView.empty();
            }

            LinkedHashMap<String, Object> parameterValues = new LinkedHashMap<>();
            parameters.forEach((key, value) -> parameterValues.put(String.valueOf(key), value));

            return new MapConfigView(parameterValues);
        }
        return new MapConfigView(values);
    }

    /**
     * Runtime固有の{@code parameters}に、表示に必要な説明キーだけを加えたビューを返す。
     *
     * <p>Ability共通メタデータをRuntimeのConfig解析へ混入させず、Presentationは
     * JSONで宣言された{@code descriptionKey}を引き続き利用できる。</p>
     */
    public static ConfigView forPresentation(Object bean) {
        if (bean == null) {
            return ConfigView.empty();
        }

        ConfigView runtimeConfig = of(bean);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();

        if (runtimeConfig instanceof MapConfigView mapConfig) {
            values.putAll(mapConfig.values);
        }

        try {
            Field description = bean.getClass().getField("descriptionKey");
            values.put("descriptionKey", description.get(bean));

        } catch (NoSuchFieldException ignored) {
            // Ability以外のDTOではRuntime用ビューをそのまま返す。

        } catch (IllegalAccessException error) {
            throw new IllegalArgumentException("Cannot expose Ability presentation configuration", error);

        }

        return values.isEmpty() ? ConfigView.empty() : new MapConfigView(values);
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Object value(String key) {
        return values.get(key);
    }

    private static Object sanitize(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            map.forEach((key, child) -> nested.put(String.valueOf(key), sanitize(child)));
            return new MapConfigView(nested);
        }

        if (value instanceof List<?> list) {
            return list.stream().map(MapConfigView::sanitize).toList();
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> copy = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                copy.add(sanitize(Array.get(value, i)));
            }
            return List.copyOf(copy);

        }
        throw new IllegalArgumentException("Unsupported Ability parameter value: " + value.getClass().getName());
    }
}
