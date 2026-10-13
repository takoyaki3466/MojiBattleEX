package jp.takoa.mojibattle.domain.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** 補助キャラクター定義をID順序を維持して登録・検索するレジストリ。 */
public final class SupportRegistry {
    private final Map<String, SupportCharacterDefinition> values = new LinkedHashMap<>();

    /**
     * 定義を登録する。
     *
     * @param value 登録する定義
     * @throws IllegalArgumentException 同じIDが既に登録済みの場合
     */
    public void register(SupportCharacterDefinition value) {
        if (values.putIfAbsent(value.id, value) != null) {
            throw new IllegalArgumentException("Duplicate support: " + value.id);
        }
    }

    /**
     * IDに対応する定義を返す。
     *
     * @param id 補助キャラクターID
     * @return 登録済みの定義
     * @throws IllegalArgumentException IDが登録されていない場合
     */
    public SupportCharacterDefinition require(String id) {
        SupportCharacterDefinition value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown support: " + id);
        }
        return value;
    }

    /** @return 登録順を維持した全定義のビュー */
    public Collection<SupportCharacterDefinition> all() {
        return values.values();
    }

    /** 同じ保存枠のカスタム定義を新しい定義へ置き換える。 */
    public void replaceCustom(SupportCharacterDefinition definition) {
        values.values().removeIf(value -> value.custom && value.customSlot == definition.customSlot);
        values.put(definition.id, definition);
    }

    /** 指定した保存枠に由来するカスタム定義を削除する。 */
    public void removeCustomSlot(int slot) {
        values.values().removeIf(value -> value.custom && value.customSlot == slot);
    }
}
