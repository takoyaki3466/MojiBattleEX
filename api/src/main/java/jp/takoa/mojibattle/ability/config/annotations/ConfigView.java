package jp.takoa.mojibattle.ability.config.annotations;

import java.util.List;

/**
 * Ability固有設定を、JSONライブラリに依存せず読み取るためのビュー。
 * 値は{@link String}、{@link Number}、{@link Boolean}、{@code ConfigView}、
 * {@link List}または{@code null}だけで構成される。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.runtime.AbilityFactory}がRuntime生成時に受け取り、
 * 補助能力では{@link jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext}と
 * {@link jp.takoa.mojibattle.ability.context.SupportPassiveContext}からも参照される。
 * ホスト側のJSONアダプターがこのインターフェースを実装する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * float power = config.floatValue("power", 10f);
 * boolean homing = config.booleanValue("homing", true);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>API利用側へ特定のJSON実装を公開せず、設定形式やホスト実装を交換できるようにするために作成された。
 * 型が異なる値は例外ではなく指定した既定値へフォールバックする。</p>
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

    /**
     * 値を一つも持たない共有実装。設定省略時のnull代替として使用する。
     */
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
