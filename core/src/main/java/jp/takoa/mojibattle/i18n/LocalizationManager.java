package jp.takoa.mojibattle.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/** 言語別JSONを読み込み、キー参照と引数展開を提供するローカライズ辞書。 */
public final class LocalizationManager {
    private final Map<String, String> values = new HashMap<>();

    /**
     * 指定言語の内部リソースを読み込む。
     *
     * @param language {@code i18n/<language>.json} の言語部分
     */
    public LocalizationManager(String language) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal("i18n/" + language + ".json"));

        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            values.put(entry.name, entry.asString());
        }
    }

    /**
     * キーの文言へ {@link MessageFormat} 形式の引数を展開して返す。
     *
     * @throws IllegalArgumentException キーが辞書に存在しない場合
     */
    public String text(String key, Object... arguments) {
        String value = values.get(key);

        if (value == null) {
            throw new IllegalArgumentException("Missing i18n key: " + key);
        }

        return MessageFormat.format(value, arguments);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    /** @return フォント生成へ渡すため、全翻訳文を連結した文字列 */
    public String allCharacters() {
        return String.join("", values.values());
    }
}
