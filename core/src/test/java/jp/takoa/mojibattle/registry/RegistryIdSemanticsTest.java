package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.content.FieldEffectDataRegistration;
import jp.takoa.mojibattle.content.SupportCharacterDataRegistration;
import jp.takoa.mojibattle.resource.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryIdSemanticsTest {
    @Test
    void dataRegistriesTreatJsonIdsAsResourceReferences() {
        MojiBattleRegistries registries = new MojiBattleRegistries(new RegistryManager());

        assertDoesNotThrow(() -> registries.characters.register(RegistryId.parse("example:tree"),
                new CharacterDataRegistration(
                        ResourceId.parse("example:characters/tree.json"),
                        ResourceId.parse("example:abilities/tree_skill.json"),
                        ResourceId.parse("example:abilities/tree_ultimate.json"),
                        ResourceId.parse("example:abilities/tree_passive.json"),
                        "i18n")));
        assertDoesNotThrow(() -> registries.supportCharacters.register(RegistryId.parse("example:flower"),
                new SupportCharacterDataRegistration(
                        ResourceId.parse("example:support/flower.json"), "i18n")));
        assertDoesNotThrow(() -> registries.fieldEffects.register(RegistryId.parse("example:bloom"),
                new FieldEffectDataRegistration(
                        ResourceId.parse("example:field_effects/bloom_cycle.json"))));
    }

    @Test
    void dataRegistriesStillEnforceResourceOwnership() {
        MojiBattleRegistries registries = new MojiBattleRegistries(new RegistryManager());

        assertThrows(IllegalArgumentException.class,
                () -> registries.fieldEffects.register(RegistryId.parse("example:bloom"),
                        new FieldEffectDataRegistration(
                                ResourceId.parse("another_mod:field_effects/bloom.json"))));
    }

    @Test
    void intrinsicIdRegistriesStillRequireAnExactMatch() {
        Registry<TestValue> registry = new Registry<>("test values", TestValue::id);

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(RegistryId.parse("example:registered"),
                        new TestValue("example:internal")));
    }

    private record TestValue(String id) {
    }
}
