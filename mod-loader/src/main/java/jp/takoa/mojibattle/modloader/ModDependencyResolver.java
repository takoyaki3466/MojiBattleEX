package jp.takoa.mojibattle.modloader;

import jp.takoa.mojibattle.mod.GameApiVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * required依存を検証し、mod IDで安定化したトポロジカル順序を返す。
 */
final class ModDependencyResolver {
    private static final String HOST_MOD_ID = "mojibattle_game";
    private static final SemanticVersion HOST_VERSION = SemanticVersion.parse(GameApiVersion.GAME_VERSION);

    List<DiscoveredMod> resolve(List<DiscoveredMod> input) {
        Map<String, DiscoveredMod> byId = new LinkedHashMap<>();
        input.forEach(mod -> byId.put(mod.metadata().id, mod));
        validateDependencies(byId);

        List<DiscoveredMod> result = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        input.stream().sorted(Comparator.comparing(mod -> mod.metadata().id)).forEach(mod -> visit(mod, byId, visiting, visited, result));
        return List.copyOf(result);
    }

    private static void validateDependencies(Map<String, DiscoveredMod> byId) {
        for (DiscoveredMod mod : byId.values()) {
            for (Map.Entry<String, String> dependency : mod.metadata().dependencies.required.entrySet()) {
                if (HOST_MOD_ID.equals(dependency.getKey())) {
                    validateMinimum(mod.metadata().id, dependency.getKey(), dependency.getValue(), HOST_VERSION);
                    continue;
                }
                DiscoveredMod target = byId.get(dependency.getKey());
                if (target == null) {
                    throw new ModLoadingException("Missing required MOD " + dependency.getKey() + " for " + mod.metadata().id);
                }
                validateMinimum(mod.metadata().id, dependency.getKey(), dependency.getValue(), SemanticVersion.parse(target.metadata().version));
            }
            for (Map.Entry<String, String> dependency : mod.metadata().dependencies.optional.entrySet()) {
                if (HOST_MOD_ID.equals(dependency.getKey())) {
                    validateMinimum(mod.metadata().id, dependency.getKey(), dependency.getValue(), HOST_VERSION);
                    continue;
                }
                DiscoveredMod target = byId.get(dependency.getKey());
                if (target != null) {
                    validateMinimum(mod.metadata().id, dependency.getKey(), dependency.getValue(), SemanticVersion.parse(target.metadata().version));
                }
            }
        }
    }

    private static void validateMinimum(String owner, String dependencyId, String required, SemanticVersion actual) {
        if (required == null || required.isBlank()) {
            return;
        }
        if (!required.startsWith(">=")) {
            throw new ModLoadingException("Only >=x.y.z dependency ranges are supported: " + required);
        }
        SemanticVersion minimum = SemanticVersion.parse(required.substring(2));
        if (actual.compareTo(minimum) < 0) {
            throw new ModLoadingException("MOD " + owner + " requires " + dependencyId + " " + required);
        }
    }

    private static void visit(DiscoveredMod mod, Map<String, DiscoveredMod> byId, Set<String> visiting, Set<String> visited, List<DiscoveredMod> result) {
        String id = mod.metadata().id;
        if (visited.contains(id)) {
            return;
        }
        if (!visiting.add(id)) {
            throw new ModLoadingException("Circular MOD dependency detected at " + id);
        }
        Set<String> dependencies = new HashSet<>(mod.metadata().dependencies.required.keySet());
        dependencies.addAll(mod.metadata().dependencies.optional.keySet());
        dependencies.stream().filter(byId::containsKey).sorted().forEach(dependency -> visit(byId.get(dependency), byId, visiting, visited, result));
        visiting.remove(id);
        visited.add(id);
        result.add(mod);
    }
}
