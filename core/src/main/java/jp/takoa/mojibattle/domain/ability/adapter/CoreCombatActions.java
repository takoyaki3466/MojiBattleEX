package jp.takoa.mojibattle.domain.ability.adapter;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.battle.action.CombatActions;
import jp.takoa.mojibattle.battle.combatant.BattlefieldView;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.rule.DuelSpec;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageResult;
import jp.takoa.mojibattle.battle.action.spec.DelayedStrikeSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileSpec;
import jp.takoa.mojibattle.battle.action.spec.AreaAttackSpec;
import jp.takoa.mojibattle.battle.action.spec.CompanionSummonSpec;
import jp.takoa.mojibattle.battle.action.spec.DetonationSpec;
import jp.takoa.mojibattle.battle.action.spec.FallingStrikeSpec;
import jp.takoa.mojibattle.battle.action.spec.LineAttackSpec;
import jp.takoa.mojibattle.battle.action.spec.MovingHazardSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileBarrageSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectilePatternSpec;
import jp.takoa.mojibattle.battle.action.spec.SkillBarrageSpec;
import jp.takoa.mojibattle.battle.action.spec.SpinningAttackSpec;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.battle.action.spec.BattlefieldPulseSpec;
import jp.takoa.mojibattle.battle.presentation.CutInSpec;
import jp.takoa.mojibattle.battle.presentation.DomainSpec;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusStackRule;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.CashBundle;
import jp.takoa.mojibattle.domain.combat.FamilySummon;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.domain.combat.SpinningOne;
import jp.takoa.mojibattle.domain.combat.SushiBarrage;
import jp.takoa.mojibattle.domain.combat.UltimateStrike;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 公開CombatActionsをBattleControllerの検証済み操作へ接続する。 */
public final class CoreCombatActions implements CombatActions {
    private final BattleController battle;

    public CoreCombatActions(BattleController battle) {
        this.battle = battle;
    }

    @Override public BattlefieldView battlefield() { return new CoreBattlefieldView(battle); }

    @Override
    public void damage(CombatantView source, CombatantView target, float amount) {
        damage(source, target, amount, DamageType.SKILL);
    }

    @Override
    public void damage(CombatantView source, CombatantView target, float amount, DamageType type) {
        battle.applyCombatantDamage(combatant(source), combatant(target), DamageRequest.of(amount, type));
    }

    @Override
    public DamageResult damage(CombatantView source, CombatantView target, DamageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Damage request is required");
        }
        return battle.applyCombatantDamage(combatant(source), combatant(target), request);
    }

    @Override
    public void heal(CombatantView target, float amount) {
        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Heal amount must be finite and non-negative");
        }
        battle.healCombatant(combatant(target), amount);
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
        Object sourceRuntime = combatant(source);
        Object targetRuntime = combatant(target);
        StatusStackRule stacking = switch (spec.stacking()) {
            case REPLACE -> StatusStackRule.REPLACE;
            case REFRESH -> StatusStackRule.REFRESH;
            case ADD_STACK -> StatusStackRule.STACK;
        };
        StatusEffect effect = new StatusEffect(spec.id().toString(), battle.ownerOf(sourceRuntime),
                statusType(spec.type()), spec.magnitude(), spec.duration().seconds(), stacking,
                spec.maximumStacks()).tickEvery(spec.tickIntervalSeconds());
        if (spec.duration().permanentFlag()) {
            effect.permanent();
        }
        if (spec.consumption() == jp.takoa.mojibattle.battle.status.StatusConsumption.ON_DAMAGE_RECEIVED) {
            effect.consumeOnDamageReceived();
        }
        battle.statusesOf(targetRuntime).apply(effect);
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
        battle.spawnExternalProjectile(runtime(source), combatant(target), spec);
    }

    @Override
    public void spawnProjectilePattern(CombatantView source, ProjectilePatternSpec spec) {
        CharacterRuntime owner = runtime(source);
        if (!spec.pointsOnHit()) {
            battle.addSkillUltimatePoints(owner, spec.ultimatePointsPerHit() * spec.shots().size());
        }
        if (spec.activationEffectId() != null) {
            battle.emitEffect(new Vector3(owner.position).add(0, 1, 0), spec.activationEffectId().toString(), 2.8f, .32f);
        }
        if (spec.activationTexture() != null) {
            battle.emitImageEffect(new Vector3(owner.position).add(0, 1.4f, 0), spec.activationTexture().toString(),
                    string(spec.activationEffectId()), spec.activationEffectScale(), .8f);
        }
        long attackId = battle.nextAttackId();
        Vector3 forward = new Vector3(owner.facing).nor();
        Vector3 right = new Vector3(-forward.z, 0, forward.x);
        for (int index = 0; index < spec.shots().size(); index++) {
            var shot = spec.shots().get(index);
            Vector3 direction = spec.radial() ? new Vector3(
                    MathUtils.sin(MathUtils.PI2 * index / spec.shots().size()), 0,
                    MathUtils.cos(MathUtils.PI2 * index / spec.shots().size())) : new Vector3(forward);
            Vector3 start = new Vector3(owner.position).mulAdd(direction, 1f);
            if (!spec.radial()) start.mulAdd(right, shot.lateralOffset());
            start.y += 1.1f + shot.verticalOffset();
            Projectile projectile = new Projectile(attackId, owner.playerId, shot.glyph(), string(shot.texture()),
                    string(spec.hitEffectId()), start, direction.scl(spec.speed() * 1.3f), spec.radius(),
                    shot.baseDamage() * .55f, shot.stunSeconds(), shot.bindSeconds(), spec.lifetimeSeconds(),
                    spec.boomerang(), spec.boomerangDistance(), spec.pointsOnHit() ? spec.ultimatePointsPerHit() : 0)
                    .attackTraits(spec.ultimateAttack(), false).source(owner);
            if (spec.followUpTexture() != null) {
                projectile.followUp(spec.followUpGlyph(), spec.followUpTexture().toString(),
                        spec.followUpDamage() * .55f, spec.followUpRadius(), spec.followUpSpeed());
            }
            battle.spawn(projectile);
        }
        if (spec.fallingStrike() != null) spawnFallingStrike(source, spec.fallingStrike());
    }

    @Override
    public void spawnAreaAttack(CombatantView source, CombatantView target, AreaAttackSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Area attack specification is required");
        }
        battle.spawnAreaAttack(combatant(source), combatant(target), spec);
    }

    @Override
    public void spawnFallingStrike(CombatantView source, FallingStrikeSpec spec) {
        CharacterRuntime owner = runtime(source);
        var p = spec.position();
        UltimateStrike strike = new UltimateStrike(owner.playerId, new Vector3(p.x(), p.y(), p.z()),
                spec.radius(), spec.damage().baseDamage(), spec.warningSeconds(), spec.effectSeconds(),
                spec.spawnHeight(), string(spec.warningEffectId()), string(spec.impactEffectId()),
                spec.dropStyle(), string(spec.objectTexture()), spec.horizontalOffset(), spec.burnDuration(),
                spec.burnDamage(), spec.burnTickInterval(), spec.burnUltimatePointsPerTick());
        strike.startAfter(spec.startDelaySeconds());
        if (spec.damage().type() == DamageType.SKILL) strike.skillAttack();
        battle.spawn(strike);
    }

    @Override
    public void spawnLineAttack(CombatantView source, LineAttackSpec spec) {
        CharacterRuntime owner = runtime(source);
        for (int index = 0; index < spec.directions(); index++) {
            Vector3 direction = spec.directions() == 1 ? new Vector3(owner.facing) : new Vector3(
                    MathUtils.cos(MathUtils.PI2 * index / spec.directions()), 0,
                    MathUtils.sin(MathUtils.PI2 * index / spec.directions()));
            battle.spawn(new LineStrike(owner.playerId, owner.position, direction, spec.length(), spec.width(),
                    spec.damage().baseDamage(), spec.warningSeconds() + index * .035f, spec.effectSeconds(),
                    string(spec.warningEffectId()), string(spec.impactEffectId()), string(spec.objectTexture())).mainUltimate());
        }
    }

    @Override
    public void summonCompanions(CombatantView source, CompanionSummonSpec spec) {
        CharacterRuntime owner = runtime(source);
        float phase = battle.nextAttackId() * 1.37f;
        for (int index = 0; index < spec.count(); index++) {
            float angle = phase + MathUtils.PI2 * index / spec.count();
            float radius = 2.2f + (index % 2) * .8f;
            Vector3 spawn = new Vector3(owner.position).add(MathUtils.cos(angle) * radius, 0,
                    MathUtils.sin(angle) * radius);
            battle.spawn(new FamilySummon(owner.playerId, spawn, string(spec.texture()), spec.maximumHp(),
                    spec.defense(), spec.attackDamage(), spec.attackInterval(), spec.projectileSpeed(),
                    string(spec.impactEffectId()), angle, radius));
        }
    }

    @Override
    public void spawnMovingHazards(CombatantView source, MovingHazardSpec spec) {
        CharacterRuntime owner = runtime(source);
        for (int index = 0; index < spec.count(); index++) {
            float angle = -27f + index * 18f;
            Vector3 direction = new Vector3(owner.facing).rotate(Vector3.Y, angle);
            Vector3 start = new Vector3(owner.position).mulAdd(direction, index * .45f);
            battle.spawn(new CarHazard(owner.playerId, start, direction, spec.speed(), spec.radius(),
                    spec.damage().baseDamage(), spec.stunSeconds(), spec.lifetimeSeconds(),
                    battle.arenaHalfExtent(), string(spec.texture()), string(spec.impactEffectId())).mainUltimate());
        }
    }

    @Override
    public void spawnSpinningAttack(CombatantView source, SpinningAttackSpec spec) {
        CharacterRuntime owner = runtime(source);
        battle.spawn(new SpinningOne(owner.playerId, owner.position, spec.length(), spec.width(),
                spec.damage().baseDamage(), spec.healOnHit(), spec.missingHealthBonus(), spec.lifetimeSeconds(),
                string(spec.texture()), string(spec.impactEffectId())));
    }

    @Override
    public void startSkillBarrage(CombatantView source, SkillBarrageSpec spec) {
        CharacterRuntime owner = runtime(source);
        battle.spawn(new jp.takoa.mojibattle.domain.combat.SkillBarrage(
                owner, spec.skillId(), spec.count(), spec.intervalSeconds(), owner.position));
    }

    @Override
    public void startProjectileBarrage(CombatantView source, ProjectileBarrageSpec spec) {
        CharacterRuntime owner = runtime(source);
        battle.spawn(new SushiBarrage(owner, spec.count(), spec.intervalSeconds(), spec.damage().baseDamage(),
                spec.speed(), spec.lifetimeSeconds(), spec.radius(), string(spec.texture()),
                string(spec.impactEffectId()), spec.damage().ignoreDefense()));
        if (spec.enhancedNormalGlyph() != null && spec.texture() != null) {
            owner.enhanceNormalAttack(spec.enhancedNormalGlyph(), spec.texture().toString(), spec.enhancedNormalScale());
        }
    }

    @Override
    public void detonateOwnedMarkers(CombatantView source, DetonationSpec spec) {
        battle.detonateCashBundles(runtime(source), spec.damage().baseDamage(), spec.radius(),
                spec.warningSeconds(), spec.effectSeconds(), spec.incomingDamageMultiplier(),
                spec.debuffDurationSeconds(), string(spec.texture()), string(spec.warningEffectId()),
                string(spec.impactEffectId()));
    }

    @Override
    public boolean spawnDetonatableMarker(CombatantView source, ResourceId texture, int maximumOwned) {
        CharacterRuntime owner = runtime(source);
        if (texture == null || maximumOwned < 1) return false;
        long count = battle.objects().stream()
                .filter(object -> object instanceof CashBundle cash && cash.owner == owner.playerId).count();
        if (count >= maximumOwned) return false;
        long interval = (long) Math.floor(battle.elapsedSeconds());
        float angle = Math.floorMod(interval * 137L + owner.playerId.ordinal() * 71L, 360L);
        float radius = 3f + Math.floorMod(interval, 4L) * 1.3f;
        Vector3 position = new Vector3(owner.position).add(MathUtils.cosDeg(angle) * radius, 0,
                MathUtils.sinDeg(angle) * radius);
        float edge = battle.arenaHalfExtent() - .8f;
        position.x = MathUtils.clamp(position.x, -edge, edge);
        position.z = MathUtils.clamp(position.z, -edge, edge);
        battle.spawn(new CashBundle(owner.playerId, position, texture.toString()));
        return true;
    }

    @Override
    public void stun(CombatantView target, float durationSeconds) {
        requireNonNegative(durationSeconds, "Stun duration");
        runtime(target).applyStun(durationSeconds);
    }

    @Override
    public void bind(CombatantView target, float durationSeconds) {
        requireNonNegative(durationSeconds, "Bind duration");
        runtime(target).applyBind(durationSeconds);
    }

    @Override
    public void pullToward(CombatantView target, CombatantView destination, float maximumDistance,
                           float remainingDistance) {
        requireNonNegative(maximumDistance, "Pull distance");
        requireNonNegative(remainingDistance, "Remaining distance");
        CharacterRuntime moving = runtime(target);
        Vector3 direction = new Vector3(battle.positionOf(combatant(destination))).sub(moving.position);
        direction.y = 0;
        float distance = direction.len();
        if (distance > 0) moving.position.mulAdd(direction.scl(1f / distance),
                Math.min(maximumDistance, Math.max(0, distance - remainingDistance)));
    }

    @Override
    public boolean teleportBesideTarget(CombatantView mover, float distance) {
        requireNonNegative(distance, "Teleport distance");
        return battle.teleportRight(runtime(mover).playerId, distance);
    }

    @Override public void halveAllSkillCooldowns() { battle.halveAllSkillCooldowns(); }

    @Override
    public void gatherMainCombatantsAtCenter(float centerScale) {
        if (!Float.isFinite(centerScale) || centerScale < 0 || centerScale > 1) {
            throw new IllegalArgumentException("centerScale must be between zero and one");
        }
        for (CharacterRuntime target : battle.players()) {
            target.position.scl(centerScale);
            target.position.y = 0;
        }
    }

    @Override public float arenaHalfExtent() { return battle.arenaHalfExtent(); }

    @Override
    public long additionalUltimateAttackCount(CombatantView source) {
        return battle.additionalUltimateAttackCount(runtime(source));
    }

    @Override
    public void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds) {
        if (effectId == null) {
            throw new IllegalArgumentException("Effect ID is required");
        }
        battle.emitEffect(battle.positionOf(combatant(origin)), effectId.toString(), scale, durationSeconds);
    }

    @Override
    public List<CombatantView> enemiesOf(CombatantView source) {
        return battlefield().enemiesOf(source);
    }

    @Override
    public List<CombatantView> alliesOf(CombatantView source) {
        return battlefield().alliesOf(source);
    }

    @Override public void startDuel(DuelSpec spec) {
        if (spec == null) throw new IllegalArgumentException("Duel specification is required");
        battle.startDuel(spec, spec.allies().stream().map(this::combatant).toList(), combatant(spec.enemy()));
    }

    @Override public void lockNormalAttackTarget(CombatantView attacker, CombatantView target, float durationSeconds) {
        battle.lockNormalAttackTarget(combatant(attacker), combatant(target), durationSeconds);
    }

    @Override
    public void knockbackAwayFrom(CombatantView source, CombatantView target, float distance) {
        battle.knockbackAwayFrom(combatant(source), combatant(target), distance);
    }

    @Override
    public void activateDomain(CombatantView source, DomainSpec spec) {
        battle.activateDomain(combatant(source), spec);
    }

    @Override
    public boolean isDomainActive(CombatantView source, ResourceId domainId) {
        return battle.isDomainActive(combatant(source), domainId);
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
        battle.swapPositions(combatant(first), combatant(second));
    }

    @Override
    public void mirrorAcrossArenaCenter(CombatantView target) {
        battle.mirrorAcrossArenaCenter(combatant(target));
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

    private Object combatant(CombatantView view) {
        if (view instanceof CoreUnifiedCombatantView core && core.battle() == battle) return core.combatant();
        if (view instanceof CoreCombatantView core && battle.containsCombatant(core.runtime())) return core.runtime();
        throw new IllegalArgumentException("Combatant does not belong to this battle");
    }

    private static void requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static String string(ResourceId id) {
        return id == null ? null : id.toString();
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
            case BONUS_DAMAGE_MULTIPLIER -> StatusType.BONUS_DAMAGE;
        };
    }
}
