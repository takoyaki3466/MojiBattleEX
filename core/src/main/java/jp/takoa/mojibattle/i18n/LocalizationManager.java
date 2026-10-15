package jp.takoa.mojibattle.i18n;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import jp.takoa.mojibattle.resource.ClasspathResourceManager;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 言語別JSONを読み込み、キー参照と引数展開を提供するローカライズ辞書。
 */
public final class LocalizationManager {
    private final Map<String, String> values = new HashMap<>();

    /**
     * 指定言語の内部リソースを読み込む。
     *
     * @param language {@code i18n/<language>.json} の言語部分
     */
    public LocalizationManager(String language) {
        this(language, new ClasspathResourceManager(LocalizationManager.class.getClassLoader()), List.of());
    }

    /** 指定リソース層から言語辞書を読み込む。 */
    public LocalizationManager(String language, ResourceManager resources) {
        this(language, resources, List.of());
    }

    /** 標準辞書の後にMOD辞書を登録順で読み、キーの上書きを拒否する。 */
    public LocalizationManager(String language, ResourceManager resources, List<ResourceId> additionalDictionaries) {
        load(resources, new ResourceId("mojibattle", "i18n/" + language + ".json"));
        additionalDictionaries.forEach(id -> load(resources, id));
    }

    private void load(ResourceManager resources, ResourceId id) {
        JsonValue root;
        try (var stream = resources.open(ResourceKind.ASSET, id);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = new JsonReader().parse(reader);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load translation resource: " + id, error);
        }

        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            if (values.putIfAbsent(entry.name, entry.asString()) != null) {
                throw new IllegalArgumentException("Duplicate translation key " + entry.name + " in " + id);
            }
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

    /**
     * @return フォント生成へ渡すため、全翻訳文を連結した文字列
     */
    public String allCharacters() {
        return String.join("", values.values());
    }
}
