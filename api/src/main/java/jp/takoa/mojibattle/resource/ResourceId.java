package jp.takoa.mojibattle.resource;

import jp.takoa.mojibattle.registry.RegistryId;

/** MOD所有リソースを名前空間と相対パスで識別するID。 */
public record ResourceId(String namespace, String path) {
    public ResourceId {
        RegistryId validated = RegistryId.of(namespace, path);
        namespace = validated.namespace();
        path = validated.path();
        if (path.startsWith("/") || path.endsWith("/") || path.contains("//")) {
            throw new IllegalArgumentException("Resource path must be relative and normalized: " + path);
        }
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Resource path traversal is not allowed: " + path);
            }
        }
    }

    public static ResourceId parse(String value) {
        RegistryId id = RegistryId.parse(value);
        return new ResourceId(id.namespace(), id.path());
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
