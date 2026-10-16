package jp.takoa.mojibattle.data.loader;

import jp.takoa.mojibattle.ability.ConfigView;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import jp.takoa.mojibattle.resource.ResourceManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonDataLoaderTest {
    @Test
    void abilityParametersExposeNestedJsonAsImmutableApiValues() {
        String source = """
                {"parameters":{"damage":12.5,"enabled":true,"label":"test",
                "nested":{"count":3},"values":[1,"two",false]}}
                """;
        ResourceManager resources = new ResourceManager() {
            @Override
            public InputStream open(ResourceKind kind, ResourceId id) {
                return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public boolean exists(ResourceKind kind, ResourceId id) {
                return true;
            }

            @Override
            public List<ResourceId> list(ResourceKind kind, String namespace, String pathPrefix) {
                return List.of();
            }
        };

        ParameterDocument document = new JsonDataLoader(resources)
                .read(new ResourceId("test", "parameters.json"), ParameterDocument.class);
        ConfigView config = MapConfigView.of(document.parameters);

        assertEquals(12.5f, config.floatValue("damage", 0), .001f);
        assertTrue(config.booleanValue("enabled", false));
        assertEquals("test", config.stringValue("label", ""));
        assertEquals(3, config.objectValue("nested").intValue("count", 0));
        assertEquals(List.of(1L, "two", false), config.listValue("values"));
    }

    public static final class ParameterDocument {
        public Map<String, Object> parameters;
    }
}
