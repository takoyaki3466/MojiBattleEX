package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.registry.RegistryId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 検証済みDefinition IDと実行インスタンスFactoryを結び付ける不変カタログ。
 * 実行Ability自体は保存せず、キャラクターごとの {@link AbilityLoadout} を生成する。
 */
public final class AbilityRegistry {
    private final Map<String, Supplier<ISkill>> skills = new LinkedHashMap<>();
    private final Map<String, Supplier<IUltimate>> ultimates = new LinkedHashMap<>();
    private final Map<String, Supplier<IPassive>> passives = new LinkedHashMap<>();
    private boolean frozen;

    public void registerSkill(String id, Supplier<ISkill> factory) {
        register(skills, id, factory, "skill");
    }

    public void registerUltimate(String id, Supplier<IUltimate> factory) {
        register(ultimates, id, factory, "ultimate");
    }

    public void registerPassive(String id, Supplier<IPassive> factory) {
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
}
