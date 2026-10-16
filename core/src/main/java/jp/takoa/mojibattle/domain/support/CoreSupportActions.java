package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.ability.SupportActions;
import jp.takoa.mojibattle.battle.SupportNormalAttackModifier;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;
import java.util.Objects;

public final class CoreSupportActions implements SupportActions {
    private final SupportCharacterRuntime runtime;
    public CoreSupportActions(SupportCharacterRuntime runtime) { this.runtime = Objects.requireNonNull(runtime); }
    @Override public void addNormalAttackModifier(SupportNormalAttackModifier modifier) {
        removeNormalAttackModifier(modifier.id()); runtime.modifiers.add(new SupportCharacterRuntime.TimedModifier(modifier, null));
    }
    @Override public void addNormalAttackModifier(SupportNormalAttackModifier modifier, float durationSeconds) {
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0) throw new IllegalArgumentException("durationSeconds");
        removeNormalAttackModifier(modifier.id()); runtime.modifiers.add(new SupportCharacterRuntime.TimedModifier(modifier, new float[]{durationSeconds}));
    }
    @Override public List<SupportNormalAttackModifier> normalAttackModifiers() { return runtime.modifiers.stream().map(SupportCharacterRuntime.TimedModifier::modifier).toList(); }
    @Override public boolean removeNormalAttackModifier(ResourceId id) { return runtime.modifiers.removeIf(value -> value.modifier().id().equals(id)); }
    @Override public void setStateLines(List<AbilityStateLine> lines) { runtime.actionStateLines = List.copyOf(lines); }
}
