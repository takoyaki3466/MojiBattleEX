package jp.takoa.mojibattle.data;

import jp.takoa.mojibattle.registry.MojiBattleRegistries;
import jp.takoa.mojibattle.registry.CombatEffectDefinition;
import jp.takoa.mojibattle.registry.impl.PassiveImpl;
import jp.takoa.mojibattle.registry.impl.SkillImpl;
import jp.takoa.mojibattle.registry.impl.UltimateImpl;

/**
 * JSON上の安定IDを登録済み共有定義へ解決する境界。
 * namespaceなしの旧IDはRegistryIdの互換規則により同じ定義へ解決される。
 */
public final class RegistryResolver {
    private final MojiBattleRegistries registries;

    public RegistryResolver(MojiBattleRegistries registries) {
        this.registries = registries;
    }

    public SkillImpl skill(String id) {
        return registries.skillImplementations.require(id);
    }

    public UltimateImpl ultimate(String id) {
        return registries.ultimateImplementations.require(id);
    }

    public PassiveImpl passive(String id) {
        return registries.passiveImplementations.require(id);
    }

    public CombatEffectDefinition effect(String id) {
        return registries.effects.require(id);
    }
}
