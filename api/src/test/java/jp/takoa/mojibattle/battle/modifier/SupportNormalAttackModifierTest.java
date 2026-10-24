package jp.takoa.mojibattle.battle.modifier;

import jp.takoa.mojibattle.resource.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 補助通常攻撃の合成方式ごとの入力範囲を固定する。 */
final class SupportNormalAttackModifierTest {
    private static final ResourceId ID = ResourceId.parse("test:modifier");

    @Test
    void additiveIntervalAcceptsAValidReduction() {
        assertDoesNotThrow(() -> modifier(SupportNormalAttackModifier.Stacking.ADD, -.35f));
    }

    @Test
    void additiveIntervalRejectsZeroOrNegativeEffectiveMultiplier() {
        assertThrows(IllegalArgumentException.class,
                () -> modifier(SupportNormalAttackModifier.Stacking.ADD, -1f));
        assertThrows(IllegalArgumentException.class,
                () -> modifier(SupportNormalAttackModifier.Stacking.ADD, -1.01f));
    }

    @Test
    void absoluteIntervalModesStillRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> modifier(SupportNormalAttackModifier.Stacking.MULTIPLY, -.01f));
        assertThrows(IllegalArgumentException.class,
                () -> modifier(SupportNormalAttackModifier.Stacking.REPLACE, -.01f));
    }

    @Test
    void allFloatingPointInputsMustRemainFiniteAndSpreadNonNegative() {
        assertThrows(IllegalArgumentException.class, () -> new SupportNormalAttackModifier(
                ID, 0, SupportNormalAttackModifier.Stacking.ADD,
                Float.NaN, 0f, 0, 0, 0f, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SupportNormalAttackModifier(
                ID, 0, SupportNormalAttackModifier.Stacking.ADD,
                0f, Float.POSITIVE_INFINITY, 0, 0, 0f, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SupportNormalAttackModifier(
                ID, 0, SupportNormalAttackModifier.Stacking.ADD,
                0f, 0f, 0, 0, -.01f, null, null));
    }

    private static SupportNormalAttackModifier modifier(
            SupportNormalAttackModifier.Stacking stacking, float interval) {
        return new SupportNormalAttackModifier(ID, 0, stacking,
                0f, interval, 0, 0, 0f, null, null);
    }
}
