package jp.takoa.mojibattle.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** メインキャラクター定義をID順序を維持して登録・検索するレジストリ。 */
public final class CharacterRegistry {
    private final Map<String, CharacterDefinition> definitions = new LinkedHashMap<>();

    /**
     * 定義を登録する。
     *
     * @param definition 登録する定義
     * @throws IllegalArgumentException IDが未設定、または既に登録済みの場合
     */
    public void register(CharacterDefinition definition) {
        if (definition == null || definition.id == null || definition.id.isBlank()) {
            throw new IllegalArgumentException("Character ID is required");
        }
        if (definitions.putIfAbsent(definition.id, definition) != null) {
            throw new IllegalArgumentException("Duplicate character ID: " + definition.id);
        }
    }

    /**
     * IDに対応する定義を返す。
     *
     * @param id キャラクターID
     * @return 登録済みの定義
     * @throws IllegalArgumentException IDが登録されていない場合
     */
    public CharacterDefinition require(String id) {
        CharacterDefinition result = definitions.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Unknown character: " + id);
        }
        return result;
    }

    /** @return 登録順を維持した全定義のビュー */
    public Collection<CharacterDefinition> all() {
        return definitions.values();
    }

    /**
     * 同じ保存枠のカスタム定義を取り除き、新しい定義へ置き換える。
     *
     * @param definition 置換後のカスタム定義
     */
    public void replaceCustom(CharacterDefinition definition) {
        definitions.values().removeIf(value -> value.custom && value.customSlot == definition.customSlot);
        definitions.put(definition.id, definition);
    }

    /**
     * 指定した保存枠に由来するカスタム定義を削除する。
     *
     * @param slot 保存枠番号
     */
    public void removeCustomSlot(int slot) {
        definitions.values().removeIf(value -> value.custom && value.customSlot == slot);
    }
}
