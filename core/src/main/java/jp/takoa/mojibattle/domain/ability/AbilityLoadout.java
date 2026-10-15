package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.CharacterDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1体の実行キャラクターだけが所有するAbilityインスタンス群。
 *
 * <p>同じDefinitionを装備した別プレイヤーとはインスタンスを共有しない。必殺技から参照される
 * 追加スキルも、このloadout内だけで遅延生成して再利用する。</p>
 */
public final class AbilityLoadout {
    private final Map<String, Supplier<ISkill>> skillFactories;
    private final Map<String, ISkill> skills = new LinkedHashMap<>();
    private final IUltimate ultimate;
    private final List<IPassive> passives;
    private final String primarySkillId;

    AbilityLoadout(CharacterDefinition definition,
                   Map<String, Supplier<ISkill>> skillFactories,
                   Map<String, Supplier<IUltimate>> ultimateFactories,
                   Map<String, Supplier<IPassive>> passiveFactories) {
        this.skillFactories = skillFactories;
        primarySkillId = definition.skillId;
        ultimate = requireFactory(ultimateFactories, definition.ultimateId, "ultimate").get();
        passives = java.util.Arrays.stream(definition.equippedPassiveIds())
                .map(id -> requireFactory(passiveFactories, id, "passive").get())
                .toList();
        skill(primarySkillId);
    }

    public ISkill primarySkill() {
        return skill(primarySkillId);
    }

    /** このプレイヤー専用のスキルを返し、初回参照時だけFactoryを評価する。 */
    public ISkill skill(String id) {
        return skills.computeIfAbsent(id, key -> requireFactory(skillFactories, key, "skill").get());
    }

    public IUltimate ultimate() {
        return ultimate;
    }

    public List<IPassive> passives() {
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
