package jp.takoa.mojibattle.domain.ability;

import java.util.HashMap;
import java.util.Map;

public final class AbilityRegistry {
    private final Map<String, ISkill> skills = new HashMap<>();
    private final Map<String, IUltimate> ultimates = new HashMap<>();
    private final Map<String, IPassive> passives = new HashMap<>();
    public void register(ISkill value) { duplicate(skills.putIfAbsent(value.id(), value), value.id()); }
    public void register(IUltimate value) { duplicate(ultimates.putIfAbsent(value.id(), value), value.id()); }
    public void register(IPassive value) { duplicate(passives.putIfAbsent(value.id(), value), value.id()); }
    private static void duplicate(Object previous, String id) { if (previous != null) throw new IllegalArgumentException("Duplicate ability ID: " + id); }
    public ISkill skill(String id) { ISkill v = skills.get(id); if (v == null) throw new IllegalArgumentException("Unknown skill: " + id); return v; }
    public IUltimate ultimate(String id) { IUltimate v = ultimates.get(id); if (v == null) throw new IllegalArgumentException("Unknown ultimate: " + id); return v; }
    public IPassive passive(String id) { IPassive v = passives.get(id); if (v == null) throw new IllegalArgumentException("Unknown passive: " + id); return v; }
}
