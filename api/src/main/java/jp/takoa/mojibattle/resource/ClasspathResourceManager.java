package jp.takoa.mojibattle.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 名前空間化リソースと従来のルート配置をクラスパスから読む互換実装。
 */
public final class ClasspathResourceManager implements ResourceManager {
    private static final String HOST_NAMESPACE = "mojibattle";
    private final ClassLoader classLoader;

    public ClasspathResourceManager(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("Class loader is required");
        }
        this.classLoader = classLoader;
    }

    @Override
    public InputStream open(ResourceKind kind, ResourceId id) throws IOException {
        String namespaced = kind.directory() + "/" + id.namespace() + "/" + id.path();
        InputStream stream = classLoader.getResourceAsStream(namespaced);

        if (stream == null && HOST_NAMESPACE.equals(id.namespace())) {
            stream = classLoader.getResourceAsStream(id.path());
        }
        if (stream == null) {
            throw new IOException("Missing classpath resource: " + kind + " " + id);
        }
        return stream;
    }

    @Override
    public boolean exists(ResourceKind kind, ResourceId id) {
        try (InputStream ignored = open(kind, id)) {
            return true;

        } catch (IOException error) {
            return false;
        }
    }

    @Override
    public List<ResourceId> list(ResourceKind kind, String namespace, String pathPrefix) {
        return List.of();
    }
}
