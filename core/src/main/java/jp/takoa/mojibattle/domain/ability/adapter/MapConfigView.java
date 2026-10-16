package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.ConfigView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/** JSONローダーのMap表現をAPIの不変値だけへ変換する境界。 */
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
        throw new IllegalArgumentException("Unsupported Ability parameter value: " + value.getClass().getName());
    }
}
