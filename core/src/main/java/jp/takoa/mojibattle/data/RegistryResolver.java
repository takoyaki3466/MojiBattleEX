package jp.takoa.mojibattle.data;

import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.CombatEffectDefinition;
import jp.takoa.mojibattle.registry.PassiveImplementation;
import jp.takoa.mojibattle.registry.SkillImplementation;
import jp.takoa.mojibattle.registry.UltimateImplementation;

/**
 * JSON上の安定IDを登録済み共有定義へ解決する境界。
 * namespaceなしの旧IDはRegistryIdの互換規則により同じ定義へ解決される。
 */
public final class RegistryResolver {
    private final BuiltInRegistries registries;

    public RegistryResolver(BuiltInRegistries registries) {
        this.registries = registries;
    }

    public SkillImplementation skill(String id) {
        return registries.skillImplementations.require(id);
    }

    public UltimateImplementation ultimate(String id) {
        return registries.ultimateImplementations.require(id);
    }

    public PassiveImplementation passive(String id) {
        return registries.passiveImplementations.require(id);
    }

    public CombatEffectDefinition effect(String id) {
        return registries.effects.require(id);
    }
}
