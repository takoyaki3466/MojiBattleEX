package jp.takoa.mojibattle.data.loader;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 名前空間化されたデータリソースからJSONを読み、欠落と構文不正をパス付き例外へ変換する。
 * 値の意味検証やRegistry解決は担当せず、デシリアライズだけを行う。
 */
public final class JsonDataLoader {
    private final Json json = new Json();
    private final ResourceManager resources;

    public JsonDataLoader(ResourceManager resources) {
        if (resources == null) {
            throw new IllegalArgumentException("Resource manager is required");
        }
        this.resources = resources;
        json.setSerializer(Map.class, new Json.Serializer<>() {
            @Override
            public void write(Json json, Map object, Class knownType) {
                json.writeObjectStart();
                object.forEach((key, value) -> json.writeValue(String.valueOf(key), value));
                json.writeObjectEnd();
            }

            @Override
            public Map<String, Object> read(Json json, JsonValue data, Class type) {
                return readObject(data);
            }
        });
    }

    /**
     * 標準MOD名前空間のJSONを指定型へデシリアライズする。
     *
     * @param path 標準MODのdataルートからの相対パス。旧ルート配置にも対応する
     * @param type デシリアライズ先の型
     * @return JSONから生成した新しいデータオブジェクト
     * @throws SerializationException ファイル欠落、構文不正、または型変換失敗の場合
     */
    public <T> T read(String path, Class<T> type) {
        return read(new ResourceId("mojibattle", path), type);
    }

    /**
     * 任意MOD名前空間のJSONをデシリアライズする。
     */
    public <T> T read(ResourceId id, Class<T> type) {
        try (var stream = resources.open(ResourceKind.DATA, id); var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return json.fromJson(type, reader);
        } catch (IOException | RuntimeException error) {
            throw new SerializationException("Invalid config " + id, error);
        }
    }

    private static Map<String, Object> readObject(JsonValue value) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (JsonValue child = value.child; child != null; child = child.next) {
            result.put(child.name, readValue(child));
        }
        return result;
    }

    private static Object readValue(JsonValue value) {
        if (value.isObject()) {
            return readObject(value);
        }
        if (value.isArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonValue child = value.child; child != null; child = child.next) {
                result.add(readValue(child));
            }
            return List.copyOf(result);
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isLong()) {
            return value.asLong();
        }
        if (value.isDouble()) {
            return value.asDouble();
        }
        return null;
    }
}
