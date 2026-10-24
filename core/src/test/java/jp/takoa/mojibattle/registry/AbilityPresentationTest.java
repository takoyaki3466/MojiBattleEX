package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbilityPresentationTest {
    @Test
    void defaultPresentationUsesJsonDescriptionKey() {
        AbilityPresentation presentation = AbilityPresentation.fromDescription(
                MapConfigView.of(Map.of("descriptionKey", "ability.expansion_mod.tea.skill.description")),
                12f);

        assertEquals("ability.expansion_mod.tea.skill.description", presentation.lines().getFirst().key());
        assertEquals(12f, presentation.lines().getFirst().arguments()[0]);
    }

    @Test
    void missingDescriptionKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AbilityPresentation.fromDescription(MapConfigView.of(Map.of())));
    }
}
