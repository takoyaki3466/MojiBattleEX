package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.DamageType;
import jp.takoa.mojibattle.battle.DamageRequest;
import jp.takoa.mojibattle.battle.DamageResult;
import jp.takoa.mojibattle.battle.DelayedStrikeSpec;
import jp.takoa.mojibattle.battle.ProjectileSpec;
import jp.takoa.mojibattle.battle.AreaAttackSpec;
import jp.takoa.mojibattle.battle.StatusEffectType;
import jp.takoa.mojibattle.battle.StatusSpec;
import jp.takoa.mojibattle.battle.BattlefieldPulseSpec;
import jp.takoa.mojibattle.battle.CutInSpec;
import jp.takoa.mojibattle.battle.DomainSpec;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusStackRule;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 公開CombatActionsをBattleControllerの検証済み操作へ接続する。 */
public final class CoreCombatActions implements CombatActions {
    private final BattleController battle;

    public CoreCombatActions(BattleController battle) {
        this.battle = battle;
    }

    @Override
    public void damage(CombatantView source, CombatantView target, float amount) {
        damage(source, target, amount, DamageType.SKILL);
    }

    @Override
    public void damage(CombatantView source, CombatantView target, float amount, DamageType type) {
        battle.applyAbilityDamage(runtime(source), runtime(target), amount, type);
    }

    @Override
    public DamageResult damage(CombatantView source, CombatantView target, DamageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Damage request is required");
        }
        return battle.applyAbilityDamage(runtime(source), runtime(target), request);
    }

    @Override
    public void heal(CombatantView target, float amount) {
        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Heal amount must be finite and non-negative");
        }
        runtime(target).heal(amount);
    }

    @Override
    public void addUltimatePoints(CombatantView target, float points) {
        requireNonNegative(points, "Ultimate points");
        battle.addExternalUltimatePoints(runtime(target), points);
    }

    @Override
    public void refundUltimatePoints(CombatantView target, float points) {
        requireNonNegative(points, "Ultimate point refund");
        battle.refundUltimatePoints(runtime(target), points);
    }

    @Override
    public void applyStatus(CombatantView source, CombatantView target, StatusSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Status specification is required");
        }
        CharacterRuntime sourceRuntime = runtime(source);
        CharacterRuntime targetRuntime = runtime(target);
        StatusStackRule stacking = switch (spec.stacking()) {
            case REPLACE -> StatusStackRule.REPLACE;
            case REFRESH -> StatusStackRule.REFRESH;
            case ADD_STACK -> StatusStackRule.STACK;
        };
        StatusEffect effect = new StatusEffect(spec.id().toString(), sourceRuntime.playerId,
                statusType(spec.type()), spec.magnitude(), spec.duration().seconds(), stacking,
                spec.maximumStacks()).tickEvery(spec.tickIntervalSeconds());
        if (spec.duration().permanentFlag()) {
            effect.permanent();
        }
        if (spec.consumption() == jp.takoa.mojibattle.battle.StatusConsumption.ON_DAMAGE_RECEIVED) {
            effect.consumeOnDamageReceived();
        }
        targetRuntime.statuses.apply(effect);
    }

    @Override
    public void spawnDelayedStrike(CombatantView source, CombatantView target, DelayedStrikeSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Delayed strike specification is required");
        }
        battle.spawnDelayedStrike(runtime(source), runtime(target), spec);
    }

    @Override
    public void spawnProjectile(CombatantView source, CombatantView target, ProjectileSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Projectile specification is required");
        }
        battle.spawnExternalProjectile(runtime(source), runtime(target), spec);
    }

    @Override
    public void spawnAreaAttack(CombatantView source, CombatantView target, AreaAttackSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Area attack specification is required");
        }
        battle.spawnAreaAttack(runtime(source), runtime(target), spec);
    }

    @Override
    public void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds) {
        if (effectId == null) {
            throw new IllegalArgumentException("Effect ID is required");
        }
        battle.emitEffect(runtime(origin).position, effectId.toString(), scale, durationSeconds);
    }

    @Override
    public List<CombatantView> enemiesOf(CombatantView source) {
        return battle.enemiesOf(runtime(source)).stream()
                .map(CoreCombatantView::new)
                .map(CombatantView.class::cast)
                .toList();
    }

    @Override
    public List<CombatantView> alliesOf(CombatantView source) {
        return battle.alliesOf(runtime(source)).stream()
                .map(CoreCombatantView::new)
                .map(CombatantView.class::cast)
                .toList();
    }

    @Override
    public void knockbackAwayFrom(CombatantView source, CombatantView target, float distance) {
        battle.knockbackAwayFrom(runtime(source), runtime(target), distance);
    }

    @Override
    public void activateDomain(CombatantView source, DomainSpec spec) {
        battle.activateDomain(runtime(source), spec);
    }

    @Override
    public boolean isDomainActive(CombatantView source, ResourceId domainId) {
        return battle.isDomainActive(runtime(source), domainId);
    }

    @Override
    public void showCutIn(CombatantView source, CutInSpec spec) {
        battle.showCutIn(runtime(source), spec);
    }

    @Override
    public void startBattlefieldPulse(CombatantView source, BattlefieldPulseSpec spec) {
        battle.startBattlefieldPulse(runtime(source), spec);
    }

    @Override
    public void swapPositions(CombatantView first, CombatantView second) {
        battle.swapPositions(runtime(first), runtime(second));
    }

    @Override
    public void mirrorAcrossArenaCenter(CombatantView target) {
        battle.mirrorAcrossArenaCenter(runtime(target));
    }

    @Override
    public void cleanseNegativeStatuses(CombatantView target) {
        runtime(target).clearNegativeStatuses();
    }

    private CharacterRuntime runtime(CombatantView view) {
        if (!(view instanceof CoreCombatantView core) || !battle.containsCombatant(core.runtime())) {
            throw new IllegalArgumentException("Combatant does not belong to this battle");
        }
        return core.runtime();
    }

    private static void requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static StatusType statusType(StatusEffectType type) {
        return switch (type) {
            case ATTACK_MULTIPLIER -> StatusType.ATTACK_UP;
            case INCOMING_DAMAGE_MULTIPLIER -> StatusType.DAMAGE_TAKEN_UP;
            case MOVE_SPEED_MULTIPLIER -> StatusType.MOVE_UP;
            case CRITICAL_RATE_ADDITION -> StatusType.CRITICAL_RATE_UP;
            case CRITICAL_DAMAGE_MULTIPLIER -> StatusType.CRITICAL_DAMAGE_UP;
            case ULTIMATE_GAIN_MULTIPLIER -> StatusType.ULTIMATE_GAIN_UP;
            case SKILL_COOLDOWN_MULTIPLIER -> StatusType.COOLDOWN_REDUCTION;
            case NORMAL_ATTACK_INTERVAL_MULTIPLIER -> StatusType.NORMAL_ATTACK_INTERVAL;
            case DAMAGE_OVER_TIME -> StatusType.BURN;
            case HEAL_OVER_TIME -> StatusType.HEAL_OVER_TIME;
            case NEXT_INCOMING_DAMAGE_MULTIPLIER -> StatusType.DAMAGE_TAKEN_DOWN;
        };
    }
}
