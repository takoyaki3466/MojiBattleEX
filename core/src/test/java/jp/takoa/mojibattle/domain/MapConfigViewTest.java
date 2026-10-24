package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapConfigViewTest {
    @Test
    void parameterizedAbilityUsesExplicitParameterInsteadOfDtoDefault() {
        ParameterizedAbility ability = new ParameterizedAbility();
        ability.parameters = Map.of("projectileSpeed", 12f);

        ConfigView view = MapConfigView.of(ability);

        assertTrue(view.contains("projectileSpeed"));
        assertEquals(12f, view.value("projectileSpeed"));
    }

    @Test
    void omittedParameterRemainsAbsentForAnnotationDefault() {
        ConfigView view = MapConfigView.of(new ParameterizedAbility());

        assertFalse(view.contains("projectileSpeed"));
        assertFalse(view.contains("cooldownSeconds"));
    }

    @Test
    void omittedParametersObjectAlsoUsesAnnotationDefaults() {
        ParameterizedAbility ability = new ParameterizedAbility();
        ability.parameters = null;

        ConfigView view = MapConfigView.of(ability);

        assertFalse(view.contains("projectileSpeed"));
    }

    @Test
    void explicitZeroRemainsPresentForRangeValidation() {
        ParameterizedAbility ability = new ParameterizedAbility();
        ability.parameters = Map.of("projectileSpeed", 0f);

        ConfigView view = MapConfigView.of(ability);

        assertTrue(view.contains("projectileSpeed"));
        assertEquals(0f, view.value("projectileSpeed"));
    }

    @Test
    void presentationIncludesDescriptionWithoutCommonNumericMetadata() {
        ParameterizedAbility ability = new ParameterizedAbility();
        ability.parameters = Map.of("projectileSpeed", 12f);

        ConfigView view = MapConfigView.forPresentation(ability);

        assertEquals("ability.test.description", view.value("descriptionKey"));
        assertEquals(12f, view.value("projectileSpeed"));
        assertFalse(view.contains("cooldownSeconds"));
    }

    @Test
    void legacyAbilityStillUsesDtoFields() {
        ConfigView view = MapConfigView.of(new LegacyAbility());

        assertTrue(view.contains("projectileSpeed"));
        assertEquals(9f, view.value("projectileSpeed"));
    }

    public static final class ParameterizedAbility {
        public String implementationId = "expansion_mod:test";
        public String descriptionKey = "ability.test.description";
        public float cooldownSeconds = 7f;
        public float projectileSpeed;
        public Map<String, Object> parameters = Map.of();
    }

    public static final class LegacyAbility {
        public String implementationId = "projectile";
        public String type = "example:display_type";
        public float projectileSpeed = 9f;
        public Map<String, Object> parameters;
    }
}
