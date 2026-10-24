package jp.takoa.mojibattle.modloader;

import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.resource.ResourceKind;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ホストの互換リソースと、MOD JAR内の名前空間化リソースを読み出す。
 */
public final class ModResourceManager implements ResourceManager {
    private static final String HOST_NAMESPACE = "mojibattle";
    private final ClassLoader hostClassLoader;
    private final Map<String, Path> modJars;

    ModResourceManager(ClassLoader hostClassLoader, List<DiscoveredMod> mods) {
        this.hostClassLoader = hostClassLoader;
        Map<String, Path> jars = new LinkedHashMap<>();
        mods.forEach(mod -> jars.put(mod.metadata().id, mod.jar()));
        modJars = Map.copyOf(jars);
    }

    @Override
    public InputStream open(ResourceKind kind, ResourceId id) throws IOException {
        Path jarPath = modJars.get(id.namespace());
        String entryName = entryName(kind, id);
        if (jarPath != null) {
            JarFile jar = new JarFile(jarPath.toFile());
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null && HOST_NAMESPACE.equals(id.namespace())) {
                entry = jar.getJarEntry(id.path());
            }
            if (entry == null || entry.isDirectory()) {
                jar.close();
                throw new IOException("Missing MOD resource: " + kind + " " + id);
            }
            InputStream stream = jar.getInputStream(entry);
            return new FilterInputStream(stream) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        jar.close();
                    }
                }
            };
        }

        InputStream stream = hostClassLoader.getResourceAsStream(entryName);
        if (stream == null && HOST_NAMESPACE.equals(id.namespace())) {
            stream = hostClassLoader.getResourceAsStream(id.path());
        }
        if (stream == null) {
            throw new IOException("Missing host resource: " + kind + " " + id);
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
    public List<ResourceId> list(ResourceKind kind, String namespace, String pathPrefix) throws IOException {
        String normalizedNamespace = new ResourceId(namespace, "validation").namespace();
        String normalizedPrefix = normalizePrefix(pathPrefix);
        Path jarPath = modJars.get(normalizedNamespace);
        if (jarPath == null) {
            return List.of();
        }
        String root = kind.directory() + "/" + normalizedNamespace + "/" + normalizedPrefix;
        List<ResourceId> result = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(root)) {
                    String path = entry.getName().substring((kind.directory() + "/" + normalizedNamespace + "/").length());
                    result.add(new ResourceId(normalizedNamespace, path));
                }
            }
        }
        result.sort(Comparator.comparing(ResourceId::path));
        return List.copyOf(result);
    }

    private static String entryName(ResourceKind kind, ResourceId id) {
        return kind.directory() + "/" + id.namespace() + "/" + id.path();
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.trim().toLowerCase(Locale.ROOT);
        String withoutTrailingSlash = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        new ResourceId("validation", withoutTrailingSlash);
        return withoutTrailingSlash + "/";
    }
}
