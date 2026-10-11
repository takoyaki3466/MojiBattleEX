package jp.takoa.mojibattle.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CharacterRegistry {
    private final Map<String, CharacterDefinition> definitions = new LinkedHashMap<>();

    public void register(CharacterDefinition definition) {
        if (definition == null || definition.id == null || definition.id.isBlank()) throw new IllegalArgumentException("Character ID is required");
        if (definitions.putIfAbsent(definition.id, definition) != null) throw new IllegalArgumentException("Duplicate character ID: " + definition.id);
    }

    public CharacterDefinition require(String id) {
        CharacterDefinition result = definitions.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown character: " + id);
        return result;
    }

    public Collection<CharacterDefinition> all() { return definitions.values(); }
}
