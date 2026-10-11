package jp.takoa.mojibattle.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public final class LocalizationManager {
    private final Map<String, String> values = new HashMap<>();

    public LocalizationManager(String language) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal("i18n/" + language + ".json"));
        for (JsonValue entry = root.child; entry != null; entry = entry.next) values.put(entry.name, entry.asString());
    }

    public String text(String key, Object... arguments) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("Missing i18n key: " + key);
        return MessageFormat.format(value, arguments);
    }

    public boolean contains(String key) { return values.containsKey(key); }
    public String allCharacters() { return String.join("", values.values()); }
}
