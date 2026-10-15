package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.registry.FieldEffectImplementation;

/**
 * フィールド効果共通値の検証を、登録宣言から分離して提供する。
 */
public final class FieldEffectImplementations {
    private FieldEffectImplementations() {
    }

    public static FieldEffectImplementation create(String id) {
        return new FieldEffectImplementation(id, effect -> {
            if (effect.magnitude < 0 || effect.intervalSeconds < 0 || effect.durationSeconds < 0 || effect.maximum < 0) {
                throw new IllegalArgumentException("Field-effect values are invalid: " + effect.id);
            }
        });
    }
}
