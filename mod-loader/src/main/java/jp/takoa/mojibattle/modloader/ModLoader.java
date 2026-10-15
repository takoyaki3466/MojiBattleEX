package jp.takoa.mojibattle.modloader;

import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 外部MODを検証し、共有の専用ClassLoaderから依存順に生成する初期ローダー。
 */
public final class ModLoader implements AutoCloseable {
    private final Path modsDirectory;
    private URLClassLoader classLoader;
    private ResourceManager resources;
    private boolean loadAttempted;

    public ModLoader(Path modsDirectory) {
        this.modsDirectory = modsDirectory.toAbsolutePath().normalize();
    }

    /**
     * 全JARを検証してからClassLoaderを生成する。部分的にロードした一覧は返さない。
     */
    public List<LoadedMod> load() {
        if (loadAttempted) {
            throw new IllegalStateException("MOD loader can only load once");
        }
        loadAttempted = true;
        List<DiscoveredMod> discovered = new ModScanner().scan(modsDirectory);
        new ModValidator().validate(discovered);
        List<DiscoveredMod> ordered = new ModDependencyResolver().resolve(discovered);
        URL[] urls = ordered.stream().map(mod -> toUrl(mod.jar())).toArray(URL[]::new);
        classLoader = new URLClassLoader("mojibattle-mods", urls, GameMod.class.getClassLoader());
        resources = new ModResourceManager(GameMod.class.getClassLoader(), ordered);

        List<LoadedMod> loaded = new ArrayList<>();
        try {
            for (DiscoveredMod mod : ordered) {
                Class<?> raw = Class.forName(mod.metadata().entrypoint, true, classLoader);
                GameMod instance = raw.asSubclass(GameMod.class).getDeclaredConstructor().newInstance();
                if (!mod.metadata().id.equals(instance.id())) {
                    throw new ModLoadingException("Entrypoint MOD ID mismatch: " + mod.metadata().id + " != " + instance.id());
                }
                loaded.add(new LoadedMod(mod.metadata(), mod.jar(), instance));
            }
            return List.copyOf(loaded);
        } catch (ModLoadingException error) {
            closeAfterFailure(error);
            throw error;
        } catch (ReflectiveOperationException | LinkageError error) {
            ModLoadingException wrapped = new ModLoadingException("Cannot create MOD entrypoint", error);
            closeAfterFailure(wrapped);
            throw wrapped;
        }
    }

    /**
     * {@link #load()} 成功後に、ホストと検出済みMODのリソース名前空間を返す。
     */
    public ResourceManager resources() {
        if (resources == null) {
            throw new IllegalStateException("MOD resources are not available before load");
        }
        return resources;
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (IOException error) {
            throw new ModLoadingException("Cannot convert MOD path to URL: " + path, error);
        }
    }

    private void closeAfterFailure(RuntimeException original) {
        try {
            close();
        } catch (RuntimeException closeError) {
            original.addSuppressed(closeError);
        }
    }

    @Override
    public void close() {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException error) {
            throw new ModLoadingException("Cannot close MOD class loader", error);
        } finally {
            classLoader = null;
        }
    }
}
