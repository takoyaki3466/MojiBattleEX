package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.ability.support.SupportActions;
import jp.takoa.mojibattle.ability.support.SupportSplitSpec;
import jp.takoa.mojibattle.battle.modifier.SupportNormalAttackModifier;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;
import java.util.Objects;

public final class CoreSupportActions implements SupportActions {
    private final SupportCharacterRuntime runtime;
    private final BattleController battle;

    public CoreSupportActions(SupportCharacterRuntime runtime) {
        this(null, runtime);
    }

    public CoreSupportActions(BattleController battle, SupportCharacterRuntime runtime) {
        this.battle = battle;
        this.runtime = Objects.requireNonNull(runtime);
    }

    @Override
    public void addNormalAttackModifier(SupportNormalAttackModifier modifier) {
        removeNormalAttackModifier(modifier.id());
        runtime.modifiers.add(new SupportCharacterRuntime.TimedModifier(modifier, null));
    }

    @Override
    public void addNormalAttackModifier(SupportNormalAttackModifier modifier, float durationSeconds) {
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds");
        }

        removeNormalAttackModifier(modifier.id());
        runtime.modifiers.add(new SupportCharacterRuntime.TimedModifier(modifier, new float[]{durationSeconds}));
    }

    @Override
    public List<SupportNormalAttackModifier> normalAttackModifiers() {
        return runtime.modifiers.stream().map(SupportCharacterRuntime.TimedModifier::modifier).toList();
    }

    @Override
    public boolean removeNormalAttackModifier(ResourceId id) {
        return runtime.modifiers.removeIf(value -> value.modifier().id().equals(id));
    }

    @Override
    public void setStateLines(List<AbilityStateLine> lines) {
        runtime.actionStateLines = List.copyOf(lines);
    }

    @Override
    public void refundUltimatePoints(float points) {
        if (!Float.isFinite(points) || points < 0) {
            throw new IllegalArgumentException("points");
        }

        runtime.addUltimatePoints(points);
    }

    @Override
    public void splitTemporarily(SupportSplitSpec spec) {
        if (battle == null) {
            throw new UnsupportedOperationException("No battle is attached");
        }

        battle.splitSupport(runtime, spec);
    }

    @Override
    public void setBattlefieldCountAttackMultiplier(ResourceId id, int priority, float baseMultiplier, float multiplierPerCombatant) {
        Objects.requireNonNull(id, "id");

        if (!Float.isFinite(baseMultiplier) || baseMultiplier < 0 || !Float.isFinite(multiplierPerCombatant)) {
            throw new IllegalArgumentException("Invalid count multiplier");
        }

        runtime.countModifiers.put(id, new SupportCharacterRuntime.CountModifier(priority, baseMultiplier, multiplierPerCombatant));
    }
}
