package jp.takoa.mojibattle.ability;

import java.util.List;
import java.util.Map;

/**
 * Ability固有JSON設定の、JSONライブラリに依存しない読取り専用ビュー。
 * 値はString、Number、Boolean、ConfigView、Listまたはnullだけで構成される。
 */
public interface ConfigView {
    boolean contains(String key);

    Object value(String key);

    default String stringValue(String key, String fallback) {
        Object value = value(key);
        return value instanceof String text ? text : fallback;
    }

    default float floatValue(String key, float fallback) {
        Object value = value(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    default int intValue(String key, int fallback) {
        Object value = value(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    default boolean booleanValue(String key, boolean fallback) {
        Object value = value(key);
        return value instanceof Boolean flag ? flag : fallback;
    }

    default ConfigView objectValue(String key) {
        Object value = value(key);
        return value instanceof ConfigView object ? object : empty();
    }

    @SuppressWarnings("unchecked")
    default List<Object> listValue(String key) {
        Object value = value(key);
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }

    static ConfigView empty() {
        return EmptyConfigView.INSTANCE;
    }

    final class EmptyConfigView implements ConfigView {
        private static final EmptyConfigView INSTANCE = new EmptyConfigView();

        private EmptyConfigView() {
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Object value(String key) {
            return null;
        }
    }
}
