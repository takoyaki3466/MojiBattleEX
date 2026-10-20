package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.registry.impl.FieldEffectImpl;

/**
 * フィールド効果共通値の検証を、登録宣言から分離して提供する。
 */
public final class FieldEffectImplementations {
    private FieldEffectImplementations() {
    }

    public static FieldEffectImpl create(String id) {
        return new FieldEffectImpl("mojibattle:" + id, effect -> {
            if (effect.floatValue("magnitude", 0) < 0 || effect.floatValue("intervalSeconds", 0) < 0
                    || effect.floatValue("durationSeconds", 0) < 0 || effect.floatValue("maximum", 0) < 0) {
                throw new IllegalArgumentException("Field-effect values are invalid: " + effect.stringValue("id", id));
            }
        });
    }
}
