package jp.takoa.mojibattle.bootstrap;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import jp.takoa.mojibattle.registry.AbilityPresentationFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 標準itemが公開APIのDeferredRegisterだけで起動できることを固定する。
 */
final class ApiOnlyItemBootstrapTest {
    @Test
    void standardContentResolvesThroughDeferredRegisters() {
        Gdx.files = new Files() {
            public FileHandle getFileHandle(String path, FileType type) {
                return new FileHandle(path);
            }

            public FileHandle classpath(String path) {
                return new FileHandle(path);
            }

            public FileHandle internal(String path) {
                return new FileHandle(path);
            }

            public FileHandle external(String path) {
                return new FileHandle("build/test-external/" + path);
            }

            public FileHandle absolute(String path) {
                return new FileHandle(path);
            }

            public FileHandle local(String path) {
                return new FileHandle(path);
            }

            public String getExternalStoragePath() {
                return "build/test-external/";
            }

            public boolean isExternalStorageAvailable() {
                return true;
            }

            public String getLocalStoragePath() {
                return "build/test-local/";
            }

            public boolean isLocalStorageAvailable() {
                return true;
            }
        };
        var snapshot = GameBootstrap.bootstrap(List.of(new ItemContentModule()));
        try {
            assertTrue(snapshot.registries().skillImplementations.frozen());
            assertTrue(snapshot.registries().skillImplementations.contains("mojibattle:projectile"));
            assertTrue(snapshot.registries().ultimateImplementations.contains("mojibattle:area"));
            assertTrue(snapshot.registries().passiveImplementations.contains("mojibattle:compound_interest"));
            assertTrue(snapshot.registries().supportAbilityImplementations.contains("mojibattle:ally_attack_up"));

            snapshot.config().skills().forEach(definition -> AbilityPresentationFormatter.format(snapshot.registries().skillImplementations.require(definition.implementationId).presentation().apply(MapConfigView.forPresentation(definition), 1f), snapshot.i18n()));
            snapshot.config().ultimates().forEach(definition -> AbilityPresentationFormatter.format(snapshot.registries().ultimateImplementations.require(definition.implementationId).presentation().apply(MapConfigView.forPresentation(definition)), snapshot.i18n()));
            snapshot.config().passives().forEach(definition -> AbilityPresentationFormatter.format(snapshot.registries().passiveImplementations.require(definition.implementationId).presentation().apply(MapConfigView.forPresentation(definition)), snapshot.i18n()));
        } finally {
            snapshot.events().close();
            Gdx.files = null;
        }
    }
}
