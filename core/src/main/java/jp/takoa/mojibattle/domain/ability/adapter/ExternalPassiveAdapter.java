package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.PassiveRuntime;
import jp.takoa.mojibattle.ability.PassiveUpdateContext;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.ability.DamageEventContext;
import jp.takoa.mojibattle.battle.NormalAttackModifier;

import java.util.List;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.ability.IPassive;

/** 公開PassiveRuntimeを既存の倍率・通知処理へ接続する。 */
public final class ExternalPassiveAdapter implements IPassive {
    private final String id;
    private final PassiveRuntime runtime;

    public ExternalPassiveAdapter(String id, PassiveRuntime runtime) {
        this.id = id;
        this.runtime = runtime;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public float attackMultiplier(CharacterRuntime owner, float battleElapsedSeconds, int alliedSummons) {
        return 1f;
    }

    @Override
    public void onSkillActivated(BattleController battle, CharacterRuntime owner) {
        runtime.onSkillActivated(ExternalAbilityContexts.create(battle, owner));
    }

    @Override
    public void onUltimateActivated(BattleController battle, CharacterRuntime owner) {
        runtime.onUltimateActivated(ExternalAbilityContexts.create(battle, owner));
    }

    @Override
    public float ultimateDamageMultiplier(CharacterRuntime owner) {
        float value = runtime.ultimateDamageMultiplier(new CoreCombatantView(owner));
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Passive ultimate damage multiplier must be finite and non-negative: " + id);
        }
        return value;
    }

    @Override
    public float criticalRateModifier(CharacterRuntime owner) {
        float value = runtime.criticalRateModifier(new CoreCombatantView(owner));
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Passive critical rate modifier must be finite: " + id);
        }
        return value;
    }

    @Override
    public boolean ignoresDefense(CharacterRuntime owner) {
        return runtime.ignoresDefense(new CoreCombatantView(owner));
    }

    @Override
    public void update(BattleController battle, CharacterRuntime owner, float delta) {
        runtime.update(new PassiveUpdateContext(
                new CoreCombatantView(owner),
                new CoreCombatActions(battle),
                delta,
                battle.elapsedSeconds()));
    }

    @Override
    public List<NormalAttackModifier> normalAttackModifiers(CharacterRuntime owner) {
        List<NormalAttackModifier> values = runtime.normalAttackModifiers(new CoreCombatantView(owner));
        if (values == null || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Passive normal attack modifiers must not be null: " + id);
        }
        return List.copyOf(values);
    }

    @Override
    public List<AbilityStateLine> stateLines(CharacterRuntime owner) {
        List<AbilityStateLine> values = runtime.stateLines(new CoreCombatantView(owner));
        if (values == null || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Passive HUD state lines must not be null: " + id);
        }
        return List.copyOf(values);
    }

    @Override
    public void onDamageDealt(BattleController battle, CharacterRuntime source,
                              CharacterRuntime target, float amount) {
        runtime.onDamageDealt(damageContext(battle, source, target, amount));
    }

    @Override
    public void onDamageReceived(BattleController battle, CharacterRuntime source,
                                 CharacterRuntime target, float amount) {
        runtime.onDamageReceived(damageContext(battle, source, target, amount));
    }

    private static DamageEventContext damageContext(BattleController battle, CharacterRuntime source,
                                                     CharacterRuntime target, float amount) {
        return new DamageEventContext(new CoreCombatantView(source), new CoreCombatantView(target), amount,
                new CoreCombatActions(battle), battle.elapsedSeconds());
    }
}
