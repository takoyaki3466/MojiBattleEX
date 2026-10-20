package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.registry.RegistryId;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.ability.runtime.UltimateRuntime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 検証済みDefinition IDと実行インスタンスFactoryを結び付ける不変カタログ。
 * 実行Ability自体は保存せず、キャラクターごとの {@link AbilityLoadout} を生成する。
 */
public final class AbilityRegistry {
    private final Map<String, Supplier<ConfiguredSkill>> skills = new LinkedHashMap<>();
    private final Map<String, Supplier<ConfiguredUltimate>> ultimates = new LinkedHashMap<>();
    private final Map<String, Supplier<PassiveRuntime>> passives = new LinkedHashMap<>();
    private boolean frozen;

    public void registerSkill(String id, Supplier<ConfiguredSkill> factory) {
        register(skills, id, factory, "skill");
    }

    public void registerUltimate(String id, Supplier<ConfiguredUltimate> factory) {
        register(ultimates, id, factory, "ultimate");
    }

    public void registerPassive(String id, Supplier<PassiveRuntime> factory) {
        register(passives, id, factory, "passive");
    }

    /** CharacterDefinitionを基に、1体専用の実行Ability群を生成する。 */
    public AbilityLoadout createLoadout(CharacterDefinition definition) {
        if (!frozen) {
            throw new IllegalStateException("Ability factory registry must be frozen before creating loadouts");
        }
        return new AbilityLoadout(definition, Map.copyOf(skills), Map.copyOf(ultimates), Map.copyOf(passives));
    }

    public void freeze() {
        frozen = true;
    }

    private <T> void register(Map<String, Supplier<T>> values, String id, Supplier<T> factory, String kind) {
        if (frozen) {
            throw new IllegalStateException("Ability factory registry is frozen");
        }
        if (factory == null) {
            throw new IllegalArgumentException(kind + " factory is required");
        }
        String normalized = RegistryId.parse(id).toString();
        if (values.containsKey(normalized) || values.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate " + kind + " definition: " + normalized);
        }
        values.put(normalized, factory);
        if (!id.contains(":")) {
            values.put(id, factory);
        }
    }

    public record ConfiguredSkill(String id, float cooldownSeconds, int maximumStocks, int initialStocks,
                                  float stockRechargeSeconds, float healthCostRatio, SkillRuntime runtime) { }
    public record ConfiguredUltimate(String id, String nameKey, float pointCost, UltimateRuntime runtime) { }
}
