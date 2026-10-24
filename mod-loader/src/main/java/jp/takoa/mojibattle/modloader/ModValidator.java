package jp.takoa.mojibattle.modloader;

import jp.takoa.mojibattle.registry.RegistryId;
import jp.takoa.mojibattle.mod.GameApiVersion;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * metadataの必須値、ID、version、重複をClassLoader生成前に検証する。
 */
final class ModValidator {
    private static final int SUPPORTED_SCHEMA = 1;
    private static final String HOST_MOD_ID = "mojibattle_game";
    private static final SemanticVersion GAME_VERSION = SemanticVersion.parse(GameApiVersion.GAME_VERSION);

    void validate(List<DiscoveredMod> mods) {
        Set<String> ids = new HashSet<>();
        for (DiscoveredMod mod : mods) {
            ModMetadata value = mod.metadata();
            if (value == null || value.schemaVersion != SUPPORTED_SCHEMA) {
                throw new ModLoadingException("Unsupported MOD metadata schema in " + mod.jar());
            }
            try {
                String normalizedId = RegistryId.of(value.id, "validation").namespace();
                if (!normalizedId.equals(value.id)) {
                    throw new IllegalArgumentException("MOD ID must already be normalized");
                }
                if (HOST_MOD_ID.equals(value.id)) {
                    throw new IllegalArgumentException("MOD ID is reserved by the host");
                }
                SemanticVersion.parse(value.version);
                int apiMajor = Integer.parseInt(value.apiVersion);
                if (apiMajor != GameApiVersion.API_MAJOR) {
                    throw new IllegalArgumentException("Unsupported API major " + apiMajor);
                }
                validateGameVersion(value.gameVersion);
            } catch (RuntimeException error) {
                throw new ModLoadingException("Invalid metadata for " + mod.jar() + ": " + error.getMessage(), error);
            }
            if (value.name == null || value.name.isBlank() || value.entrypoint == null || value.entrypoint.isBlank()) {
                throw new ModLoadingException("MOD name and entrypoint are required: " + mod.jar());
            }
            if (!ids.add(value.id)) {
                throw new ModLoadingException("Duplicate MOD ID: " + value.id);
            }
            if (value.dependencies == null) {
                value.dependencies = new ModMetadata.Dependencies();
            }
            if (value.dependencies.required == null || value.dependencies.optional == null) {
                throw new ModLoadingException("Dependency maps are required: " + value.id);
            }
            try {
                validateDependencyMap(value.id, value.dependencies.required);
                validateDependencyMap(value.id, value.dependencies.optional);
            } catch (RuntimeException error) {
                if (error instanceof ModLoadingException loadingError) {
                    throw loadingError;
                }
                throw new ModLoadingException("Invalid dependencies for " + value.id, error);
            }
        }
    }

    private static void validateDependencyMap(String owner, Map<String, String> dependencies) {
        for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
            String normalized = RegistryId.of(dependency.getKey(), "validation").namespace();
            if (!normalized.equals(dependency.getKey()) || owner.equals(normalized)) {
                throw new ModLoadingException("Invalid dependency ID " + dependency.getKey() + " for " + owner);
            }
            String range = dependency.getValue();
            if (range != null && !range.isBlank()) {
                if (!range.startsWith(">=")) {
                    throw new ModLoadingException("Only >=x.y.z dependency ranges are supported: " + range);
                }
                SemanticVersion.parse(range.substring(2));
            }
        }
    }

    /**
     * 初期形式では完全一致または最小版指定だけを受け付ける。
     */
    private static void validateGameVersion(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("Game version is required");
        }
        if (requirement.startsWith(">=")) {
            SemanticVersion minimum = SemanticVersion.parse(requirement.substring(2));
            if (GAME_VERSION.compareTo(minimum) < 0) {
                throw new IllegalArgumentException("Game version " + requirement + " is not satisfied");
            }
            return;
        }
        SemanticVersion exact = SemanticVersion.parse(requirement);
        if (GAME_VERSION.compareTo(exact) != 0) {
            throw new IllegalArgumentException("Game version " + requirement + " is not compatible");
        }
    }
}
