package jp.takoa.mojibattle.render;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 描画キャッシュが名前空間付き参照と旧形式パスを正しく区別・解決することを検証する。 */
final class ArenaRendererResourceResolutionTest {
    @Test void namespacedReferenceResolvesLegacyCachedPath() {
        Map<String, String> models = Map.of("objects/energy-lance.png", "standard-lance");

        assertEquals("standard-lance",
                ArenaRenderer.modelForResource(models, "mojibattle:objects/energy-lance.png"));
    }

    @Test void exactNamespacedModelTakesPriorityOverLegacyPath() {
        Map<String, String> models = new HashMap<>();
        models.put("objects/energy-lance.png", "standard-lance");
        models.put("expansion_mod:objects/energy-lance.png", "expansion-lance");

        assertEquals("expansion-lance",
                ArenaRenderer.modelForResource(models, "expansion_mod:objects/energy-lance.png"));
        assertNull(ArenaRenderer.modelForResource(models, null));
    }
}
