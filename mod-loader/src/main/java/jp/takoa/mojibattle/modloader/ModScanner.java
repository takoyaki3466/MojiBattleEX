package jp.takoa.mojibattle.modloader;

import com.badlogic.gdx.utils.Json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * modsディレクトリの直下JARからmetadataを読み、API同梱を事前に拒否する。
 */
final class ModScanner {
    private static final String METADATA_PATH = "META-INF/mojibattle/mod.json";
    private static final List<String> API_PREFIXES = List.of("jp/takoa/mojibattle/battle/", "jp/takoa/mojibattle/content/", "jp/takoa/mojibattle/event/", "jp/takoa/mojibattle/mod/", "jp/takoa/mojibattle/registry/", "jp/takoa/mojibattle/resource/");
    private final Json json = new Json();

    List<DiscoveredMod> scan(Path modsDirectory) {
        if (!Files.exists(modsDirectory)) {
            return List.of();
        }
        if (!Files.isDirectory(modsDirectory)) {
            throw new ModLoadingException("Mods path is not a directory: " + modsDirectory);
        }
        try {
            List<Path> jars;
            try (var paths = Files.list(modsDirectory)) {
                jars = paths.filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path)).filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar")).sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            }
            List<DiscoveredMod> result = new ArrayList<>();
            Set<Path> realPaths = new HashSet<>();
            for (Path jar : jars) {
                Path realPath = jar.toRealPath();
                if (!realPaths.add(realPath)) {
                    throw new ModLoadingException("Duplicate MOD JAR target: " + realPath);
                }
                result.add(read(realPath));
            }
            return List.copyOf(result);
        } catch (IOException error) {
            throw new ModLoadingException("Cannot scan mods directory: " + modsDirectory, error);
        }
    }

    private DiscoveredMod read(Path path) {
        try (JarFile jar = new JarFile(path.toFile())) {
            rejectBundledApi(jar, path);
            JarEntry entry = jar.getJarEntry(METADATA_PATH);
            if (entry == null) {
                throw new ModLoadingException("Missing " + METADATA_PATH + " in " + path);
            }
            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                ModMetadata metadata = json.fromJson(ModMetadata.class, reader);
                return new DiscoveredMod(path, metadata);
            }
        } catch (ModLoadingException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw new ModLoadingException("Cannot read MOD metadata from " + path, error);
        }
    }

    private static void rejectBundledApi(JarFile jar, Path path) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (API_PREFIXES.stream().anyMatch(name::startsWith)) {
                throw new ModLoadingException("MOD must not bundle the Game API: " + path);
            }
        }
    }
}
