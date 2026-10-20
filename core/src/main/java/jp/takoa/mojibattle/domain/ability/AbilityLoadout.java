package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;

/**
 * 1体の実行キャラクターだけが所有するAbilityインスタンス群。
 *
 * <p>同じDefinitionを装備した別プレイヤーとはインスタンスを共有しない。必殺技から参照される
 * 追加スキルも、このloadout内だけで遅延生成して再利用する。</p>
 */
public final class AbilityLoadout {
    private final Map<String, Supplier<AbilityRegistry.ConfiguredSkill>> skillFactories;
    private final Map<String, AbilityRegistry.ConfiguredSkill> skills = new LinkedHashMap<>();
    private final AbilityRegistry.ConfiguredUltimate ultimate;
    private final List<PassiveRuntime> passives;
    private final String primarySkillId;

    AbilityLoadout(CharacterDefinition definition,
                   Map<String, Supplier<AbilityRegistry.ConfiguredSkill>> skillFactories,
                   Map<String, Supplier<AbilityRegistry.ConfiguredUltimate>> ultimateFactories,
                   Map<String, Supplier<PassiveRuntime>> passiveFactories) {
        this.skillFactories = skillFactories;
        primarySkillId = definition.skillId;
        ultimate = requireFactory(ultimateFactories, definition.ultimateId, "ultimate").get();
        passives = java.util.Arrays.stream(definition.equippedPassiveIds())
                .map(id -> requireFactory(passiveFactories, id, "passive").get())
                .toList();
        skill(primarySkillId);
    }

    public AbilityRegistry.ConfiguredSkill primarySkill() {
        return skill(primarySkillId);
    }

    /** このプレイヤー専用のスキルを返し、初回参照時だけFactoryを評価する。 */
    public AbilityRegistry.ConfiguredSkill skill(String id) {
        return skills.computeIfAbsent(id, key -> requireFactory(skillFactories, key, "skill").get());
    }

    public AbilityRegistry.ConfiguredUltimate ultimate() {
        return ultimate;
    }

    public List<PassiveRuntime> passives() {
        return passives;
    }

    private static <T> Supplier<T> requireFactory(Map<String, Supplier<T>> factories, String id, String kind) {
        Supplier<T> factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown " + kind + " definition: " + id);
        }
        return factory;
    }
}
