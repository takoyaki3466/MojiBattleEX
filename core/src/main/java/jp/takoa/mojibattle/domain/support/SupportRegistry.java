package jp.takoa.mojibattle.domain.support;

import java.util.Collection;

import jp.takoa.mojibattle.registry.Registry;

/**
 * 補助キャラクター定義をID順序を維持して登録・検索するレジストリ。
 */
public final class SupportRegistry {
    private final Registry<SupportCharacterDefinition> values = new Registry<>("support character", value -> value.id);

    /**
     * 定義を登録する。
     *
     * @param value 登録する定義
     * @throws IllegalArgumentException 同じIDが既に登録済みの場合
     */
    public void register(SupportCharacterDefinition value) {
        values.register(value);
    }

    /**
     * IDに対応する定義を返す。
     *
     * @param id 補助キャラクターID
     * @return 登録済みの定義
     * @throws IllegalArgumentException IDが登録されていない場合
     */
    public SupportCharacterDefinition require(String id) {
        return values.require(id);
    }

    /**
     * @return 登録順を維持した全定義のビュー
     */
    public Collection<SupportCharacterDefinition> all() {
        return values.values();
    }

    /**
     * 同じ保存枠のカスタム定義を新しい定義へ置き換える。
     */
    public void replaceCustom(SupportCharacterDefinition definition) {
        values.removeIf(value -> value.custom && value.customSlot == definition.customSlot);
        values.register(definition);
    }

    /**
     * 指定した保存枠に由来するカスタム定義を削除する。
     */
    public void removeCustomSlot(int slot) {
        values.removeIf(value -> value.custom && value.customSlot == slot);
    }
}
