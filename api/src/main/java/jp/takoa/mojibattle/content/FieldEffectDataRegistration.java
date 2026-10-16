package jp.takoa.mojibattle.content;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 外部フィールド効果JSONの登録単位。
 * @param fieldEffect MOD自身の名前空間にあるDATAリソース
 */
public record FieldEffectDataRegistration(ResourceId fieldEffect) {
    public FieldEffectDataRegistration {
        Objects.requireNonNull(fieldEffect, "fieldEffect");
    }

    public String namespace() {
        return fieldEffect.namespace();
    }
}
