package jp.takoa.mojibattle.modloader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** ゲームのAPI 1.1.0 に対する、MODの {@code apiVersion} の互換性判定を検証する。 */
final class ModValidatorApiVersionTest {
    @Test
    void acceptsLegacyMajorOnlyVersionWithTheSameMajor() {
        assertDoesNotThrow(() -> ModValidator.validateApiVersion("1"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("2"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("0"));
    }

    @Test
    void acceptsSemanticVersionsUpToTheGameApiWithinTheSameMajor() {
        assertDoesNotThrow(() -> ModValidator.validateApiVersion("1.0.0"));
        assertDoesNotThrow(() -> ModValidator.validateApiVersion("1.0.9"));
        assertDoesNotThrow(() -> ModValidator.validateApiVersion("1.1.0"));
    }

    @Test
    void rejectsNewerOrOtherMajorApiVersions() {
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("1.2.0"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("2.0.0"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("0.9.0"));
    }

    @Test
    void rejectsMissingOrMalformedVersions() {
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion(null));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion(" "));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("1.1"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("v1.1.0"));
        assertThrows(IllegalArgumentException.class, () -> ModValidator.validateApiVersion("01"));
    }
}
