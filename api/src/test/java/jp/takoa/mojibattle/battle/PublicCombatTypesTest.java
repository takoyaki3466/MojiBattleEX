package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PublicCombatTypesTest {
    @Test
    void resourceIdNormalizesAndRejectsTraversalOrAbsolutePaths() {
        assertEquals("sample:textures/glyph.png", ResourceId.parse("SAMPLE:Textures/Glyph.PNG").toString());
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("sample:../secret"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("sample:/absolute"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("sample:path//file"));
    }

    @Test
    void damageRequestRejectsNonFiniteAndNegativeDamage() {
        assertThrows(IllegalArgumentException.class,
                () -> DamageRequest.of(Float.NaN, DamageType.SKILL));
        assertThrows(IllegalArgumentException.class,
                () -> DamageRequest.of(Float.POSITIVE_INFINITY, DamageType.SKILL));
        assertThrows(IllegalArgumentException.class,
                () -> DamageRequest.of(-1f, DamageType.SKILL));
        assertThrows(NullPointerException.class,
                () -> DamageRequest.of(1f, null));
    }

    @Test
    void permanentDurationDoesNotExposeInfinity() {
        StatusDuration duration = StatusDuration.permanent();
        assertTrue(duration.permanentFlag());
        assertEquals(0f, duration.seconds());
        assertTrue(Float.isFinite(duration.seconds()));
        assertThrows(IllegalArgumentException.class, () -> StatusDuration.seconds(0));
        assertThrows(IllegalArgumentException.class,
                () -> StatusDuration.seconds(Float.POSITIVE_INFINITY));
    }

    @Test
    void statusSpecValidatesMagnitudeStacksAndTickInterval() {
        ResourceId id = ResourceId.parse("test:slow");
        StatusDuration duration = StatusDuration.seconds(3f);
        assertThrows(IllegalArgumentException.class, () -> new StatusSpec(id,
                StatusEffectType.MOVE_SPEED_MULTIPLIER, Float.NaN, duration,
                StatusStacking.REFRESH, 1, 1f));
        assertThrows(IllegalArgumentException.class, () -> new StatusSpec(id,
                StatusEffectType.MOVE_SPEED_MULTIPLIER, .5f, duration,
                StatusStacking.ADD_STACK, 0, 1f));
        assertThrows(IllegalArgumentException.class, () -> new StatusSpec(id,
                StatusEffectType.MOVE_SPEED_MULTIPLIER, .5f, duration,
                StatusStacking.ADD_STACK, 2, 0));
        assertDoesNotThrow(() -> new StatusSpec(id, StatusEffectType.CRITICAL_RATE_ADDITION,
                -.1f, duration, StatusStacking.REFRESH, 1, 1f));
    }

    @Test
    void projectileAndNormalAttackDefinitionsRejectUnsafeValues() {
        DamageRequest damage = DamageRequest.of(10f, DamageType.SKILL);
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(damage, 0, 1, 1, 1, 0, 1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileSpec(damage, 1, 1, 1, 65, 0, 1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new NormalAttackModifier(ResourceId.parse("test:bad"), 0, 0,
                        0, 0, 0, 1, null, null));
    }
}
