package jp.takoa.mojibattle.domain.ability;

import java.util.HashMap;
import java.util.Map;

/** スキル、必殺技、パッシブを種別ごとのIDで登録・検索するレジストリ。 */
public final class AbilityRegistry {
    private final Map<String, ISkill> skills = new HashMap<>();
    private final Map<String, IUltimate> ultimates = new HashMap<>();
    private final Map<String, IPassive> passives = new HashMap<>();

    public void register(ISkill value) {
        register(skills, value);
    }

    public void register(IUltimate value) {
        register(ultimates, value);
    }

    public void register(IPassive value) {
        register(passives, value);
    }

    private static <T extends IAbility> void register(Map<String, T> values, T value) {
        if (values.putIfAbsent(value.id(), value) != null) {
            throw new IllegalArgumentException("Duplicate ability ID: " + value.id());
        }
    }

    public ISkill skill(String id) {
        return require(skills, id, "skill");
    }

    public IUltimate ultimate(String id) {
        return require(ultimates, id, "ultimate");
    }

    public IPassive passive(String id) {
        return require(passives, id, "passive");
    }

    private static <T> T require(Map<String, T> values, String id, String kind) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + kind + ": " + id);
        }
        return value;
    }
}
