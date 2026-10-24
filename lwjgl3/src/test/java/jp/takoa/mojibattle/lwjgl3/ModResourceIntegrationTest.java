package jp.takoa.mojibattle.lwjgl3;

import jp.takoa.mojibattle.modloader.ModLoader;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 標準MODと外部MODを同時ロードしたときの画像名前空間を検証する。 */
final class ModResourceIntegrationTest {
    @TempDir
    Path modsDirectory;

    @Test
    void standardAndExpansionAssetsResolveOnlyFromTheirDeclaredNamespaces() throws Exception {
        String standardPath = System.getProperty("mojibattle.test.standardModJar");
        String expansionPath = System.getProperty("mojibattle.test.expansionModJar");
        assumeTrue(standardPath != null && expansionPath != null,
                "The expansion MOD JAR is required for this integration test");
        Files.copy(Path.of(standardPath), modsDirectory.resolve("standard.jar"));
        Files.copy(Path.of(expansionPath), modsDirectory.resolve("expansion.jar"));

        try (ModLoader loader = new ModLoader(modsDirectory)) {
            loader.load();
            var resources = loader.resources();

            assertReadable(resources.open(ResourceKind.ASSET,
                    ResourceId.parse("mojibattle:glyphs/south.png")));
            assertReadable(resources.open(ResourceKind.ASSET,
                    ResourceId.parse("mojibattle:glyphs/flower.png")));
            assertReadable(resources.open(ResourceKind.ASSET,
                    ResourceId.parse("expansion_mod:textures/sigma.png")));
            assertFalse(resources.exists(ResourceKind.ASSET,
                    ResourceId.parse("expansion_mod:glyphs/south.png")));
        }
    }

    private static void assertReadable(InputStream stream) throws Exception {
        try (stream) {
            assertTrue(stream.read() >= 0);
        }
    }
}
