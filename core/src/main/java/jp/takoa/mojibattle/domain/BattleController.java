package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.config.FieldEffectDefinition;
import jp.takoa.mojibattle.config.CriticalConfig;
import jp.takoa.mojibattle.config.AwakeningConfig;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.IPassive;
import jp.takoa.mojibattle.domain.combat.AwakeningItem;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.CombatObject;
import jp.takoa.mojibattle.domain.combat.FamilySummon;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.domain.combat.SkillBarrage;
import jp.takoa.mojibattle.domain.combat.SpinningOne;
import jp.takoa.mojibattle.domain.combat.SushiBarrage;
import jp.takoa.mojibattle.domain.combat.UltimateStrike;
import jp.takoa.mojibattle.domain.combat.VisualEffect;
import jp.takoa.mojibattle.domain.debug.CharacterDebugSnapshot;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.support.FieldEffectSystem;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;
import jp.takoa.mojibattle.domain.support.SupportController;
import jp.takoa.mojibattle.input.PlayerCommand;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * 固定時間ステップごとに入力、キャラクター、攻撃判定、状態効果、勝敗を更新する戦闘ドメインの中核。
 *
 * <p>描画や実キー入力には依存せず、画面層から渡されたコマンドと設定を戦闘状態へ反映する。</p>
 */
public final class BattleController {
    private static final float GLOBAL_ULTIMATE_DAMAGE_SCALE = .65f;

    /**
     * HUDへ表示する必殺技発動者、翻訳キーおよび表示時間。
     *
     * @param player 発動したプレイヤー
     * @param i18nKey 必殺技名の翻訳キー
     * @param duration 表示する秒数
     */
    public record Announcement(PlayerId player, String i18nKey, float duration) {
    }

    private final BattleConfig config;
    private final CriticalConfig criticalConfig;
    private final AwakeningConfig awakeningConfig;
    private final AbilityRegistry abilities;
    private final BattleMode mode;
    private final EnumMap<PlayerId, CharacterRuntime> players = new EnumMap<>(PlayerId.class);
    private final EnumMap<PlayerId, PlayerTeam> teams = new EnumMap<>(PlayerId.class);
    private final SupportController supportController = new SupportController();
    private final FieldEffectSystem fieldEffects = new FieldEffectSystem();
    private final Set<Long> pointAwardedAttacks = new HashSet<>();
    private final List<CombatObject> objects = new ArrayList<>();
    private final List<CombatObject> pendingObjects = new ArrayList<>();
    private final BattleTimer timer;
    private final DamageService damageService = new DamageService();
    private final BattleResultResolver resultResolver = new BattleResultResolver();
    private final TrainingStatistics trainingStatistics = new TrainingStatistics();
    private long attackSequence;
    private BattleResult result;
    private boolean paused;
    private boolean finalMinuteStarted;
    private Announcement announcement;
    private float visualPulse;
    private float criticalPulse;
    private boolean awakeningSpawned;
    private float fieldHealTimer = 10f;
    private float trafficTimer = 7f;
    private int trafficWave;
    private final Set<PlayerId> matsudoOwners = new HashSet<>();

    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<CharacterDefinition> characterDefinitions, List<SupportCharacterDefinition> supportDefinitions, List<FieldEffectDefinition> availableFieldEffects) {
        this.config = config;
        this.criticalConfig = criticalConfig;
        this.awakeningConfig = awakeningConfig;
        this.abilities = abilities;
        this.mode = mode;
        timer = new BattleTimer(config.durationSeconds);
        if (characterDefinitions.size() != supportDefinitions.size() || characterDefinitions.size() < 2 || characterDefinitions.size() > 4)
            throw new IllegalArgumentException("Battle requires 2-4 complete teams");
        Vector3[] spawns = {new Vector3(-4, config.groundY, -4), new Vector3(4, config.groundY, 4), new Vector3(-4, config.groundY, 4), new Vector3(4, config.groundY, -4)};
        for (int index = 0; index < characterDefinitions.size(); index++) {
            PlayerId id = PlayerId.values()[index];
            CharacterDefinition definition = characterDefinitions.get(index);
            SupportCharacterDefinition support = supportDefinitions.get(index);
            CharacterRuntime runtime = new CharacterRuntime(id, definition, spawns[index]);
            players.put(id, runtime);
            SupportCharacterRuntime supportRuntime = support == null ? null : new SupportCharacterRuntime(id, support, new Vector3(spawns[index]).add(index % 2 == 0 ? -2 : 2, 0, 0));
            teams.put(id, new PlayerTeam(runtime, supportRuntime));
            if (!definition.trainingDummy)
                abilities.skill(definition.skillId).initialize(runtime, config.skillCooldownMultiplier);
            if (support != null) for (FieldEffectDefinition effect : availableFieldEffects)
                if (contains(support.fieldPassiveIds, effect.id)) {
                    fieldEffects.activate(effect);
                    if ("matsudo".equals(effect.id)) matsudoOwners.add(id);
                }
        }
        fieldHealTimer = fieldEffects.interval("periodic_heal", 10f);
        trafficTimer = fieldEffects.interval("traffic", 7f);
        if (mode == BattleMode.TRAINING) players.get(PlayerId.TWO).setImmortal(true);
    }

    public void update(float delta, EnumMap<PlayerId, PlayerCommand> commands) {
        if (paused) return;
        if (result != null) {
            visualPulse = Math.max(0, visualPulse - delta * 3.2f);
            for (CombatObject object : objects) if (object instanceof VisualEffect) object.update(delta);
            objects.removeIf(CombatObject::expired);
            return;
        }
        visualPulse = Math.max(0, visualPulse - delta * 3.2f);
        criticalPulse = Math.max(0, criticalPulse - delta * 2.5f);
        timer.update(delta);
        if (!finalMinuteStarted && timer.remaining() <= config.finalMinuteSeconds) finalMinuteStarted = true;
        trainingStatistics.update(delta);
        for (CharacterRuntime current : players.values()) {
            current.update(delta);
            for (IPassive passive : passives(current)) passive.update(this, current, delta);
        }
        for (CharacterRuntime current : players.values())
            if (current.isAlive() && (mode == BattleMode.VERSUS || current.playerId == PlayerId.ONE))
                move(current, commands.getOrDefault(current.playerId, PlayerCommand.idle()), delta);
        List<CharacterRuntime> all = new ArrayList<>(players.values());
        for (int a = 0; a < all.size(); a++)
            for (int b = a + 1; b < all.size(); b++) separatePlayers(all.get(a), all.get(b));
        for (CharacterRuntime current : players.values())
            if (current.isAlive()) {
                CharacterRuntime target = targetOf(current.playerId);
                if (target != null) aimAtOpponent(current, target);
            }
        for (CharacterRuntime current : players.values())
            if (current.isAlive() && (mode == BattleMode.VERSUS || current.playerId == PlayerId.ONE))
                activate(current, commands.getOrDefault(current.playerId, PlayerCommand.idle()));
        for (CharacterRuntime current : players.values()) {
            if (current.isAlive() && team(current.playerId).support() != null) {
                CharacterRuntime target = targetOf(current.playerId);
                if (target != null)
                    supportController.update(this, team(current.playerId).support(), current, target, delta);
            }
            updateStatusDamage(current, delta);
        }
        updateFieldHealing(delta);
        updateMatsudoTraffic(delta);
        if (!awakeningSpawned && timer.remaining() <= awakeningConfig.spawnAtRemainingSeconds) spawnAwakening();
        updateObjects(delta);
        if (mode == BattleMode.VERSUS) result = resultResolver.resolve(players.values(), timer.expired());
        if (result != null) objects.removeIf(object -> !(object instanceof VisualEffect));
        if (announcement != null) {
            float next = announcement.duration - delta;
            announcement = next > 0 ? new Announcement(announcement.player, announcement.i18nKey, next) : null;
        }
    }

    private void move(CharacterRuntime player, PlayerCommand command, float delta) {
        if (player.movementLocked()) return;
        Vector2 input = command.movement();
        if (input.isZero()) return;
        Vector3 forward = new Vector3(player.facing).set(player.facing.x, 0, player.facing.z).nor();
        // Screen-right for a camera looking along forward is forward x world-up.
        Vector3 right = new Vector3(-forward.z, 0, forward.x);
        float passiveSpeed = passiveProduct(player, passive -> passive.moveSpeedMultiplier(player, timer.elapsed()));
        float fieldSpeed = fieldMoveMultiplier();
        float statusSpeed = player.statuses.multiplier(StatusType.MOVE_UP) * player.statuses.multiplier(StatusType.MOVE_DOWN);
        Vector3 movement = forward.scl(input.y).mulAdd(right, input.x).nor().scl(player.definition.moveSpeed * 1.2f * passiveSpeed * fieldSpeed * statusSpeed * (player.awakened() ? awakeningConfig.moveSpeedMultiplier : 1f) * delta);
        player.position.add(movement);
        player.position.x = MathUtils.clamp(player.position.x, -config.arenaHalfExtent, config.arenaHalfExtent);
        player.position.z = MathUtils.clamp(player.position.z, -config.arenaHalfExtent, config.arenaHalfExtent);
        player.position.y = config.groundY;
    }

    private void separatePlayers(CharacterRuntime a, CharacterRuntime b) {
        Vector3 difference = new Vector3(b.position).sub(a.position);
        difference.y = 0;
        float minimum = a.definition.collisionRadius + b.definition.collisionRadius;
        float length = difference.len();
        if (length > 0 && length < minimum) {
            Vector3 correction = difference.scl((minimum - length) / length * 0.5f);
            a.position.sub(correction);
            b.position.add(correction);
        }
    }

    private static void aimAtOpponent(CharacterRuntime source, CharacterRuntime target) {
        source.facing.set(target.position).sub(source.position).set(source.facing.x, 0, source.facing.z);
        if (!source.facing.isZero()) source.facing.nor();
    }

    private void activate(CharacterRuntime user, PlayerCommand command) {
        if (user.definition.trainingDummy) return;
        if (command.useSkill()) abilities.skill(user.definition.skillId).tryActivate(this, user);
        if (command.useUltimate()) abilities.ultimate(user.definition.ultimateId).tryActivate(this, user);
        PlayerTeam team = team(user.playerId);
        if (command.useSupportUltimate() && team.support() != null) supportController.useUltimate(this, team);
    }

    private void updateObjects(float delta) {
        for (CombatObject object : objects) {
            object.update(delta);
            if (object instanceof FamilySummon family) updateFamily(family, delta);
            else if (object instanceof Projectile projectile) resolveProjectile(projectile);
            else if (object instanceof UltimateStrike strike && strike.takeDamagePending()) resolveStrike(strike);
            else if (object instanceof LineStrike line && line.takeDamagePending()) resolveLineStrike(line);
            else if (object instanceof CarHazard car && car.damageReady()) resolveCar(car);
            else if (object instanceof SpinningOne one) {
                one.position().set(player(one.owner).position);
                if (one.damageReady()) resolveSpinningOne(one);
            } else if (object instanceof SkillBarrage barrage && barrage.takePulse())
                abilities.skill(barrage.skillId).activateFromUltimate(this, player(barrage.owner));
            else if (object instanceof SushiBarrage barrage && barrage.takeShot()) spawnSushi(barrage);
            else if (object instanceof AwakeningItem item) resolveAwakening(item);
        }
        objects.removeIf(CombatObject::expired);
        objects.addAll(pendingObjects);
        pendingObjects.clear();
    }

    private void resolveProjectile(Projectile projectile) {
        if (projectile.expired() || !projectile.hitReady()) return;
        boolean contact = false;
        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired())
                continue;
            float hitboxMultiplier = passiveHitboxMultiplier(projectile.owner);
            float familyHitRadius = projectile.radius * 3f * hitboxMultiplier + family.collisionRadius() * 1.5f;
            tempCenter.set(family.position()).add(0, 1f, 0);
            if (projectile.position().dst2(tempCenter) <= familyHitRadius * familyHitRadius) {
                contact = true;
                break;
            }
        }
        float hitboxMultiplier = passiveHitboxMultiplier(projectile.owner);
        CharacterRuntime contactTarget = null;
        for (CharacterRuntime candidate : players.values())
            if (candidate.playerId != projectile.owner && candidate.isAlive()) {
                float combined = projectile.radius * 3f * hitboxMultiplier + candidate.definition.collisionRadius * 1.5f;
                if (projectile.position().dst2(new Vector3(candidate.position).add(0, 1, 0)) <= combined * combined) {
                    contact = true;
                    contactTarget = candidate;
                    break;
                }
            }
        if (!contact) return;

        float explosionRadius = Math.max(3.2f, projectile.radius * 5f) * hitboxMultiplier;
        float explosionRadius2 = explosionRadius * explosionRadius;
        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired())
                continue;
            float familyRadius = explosionRadius + family.collisionRadius();
            tempCenter.set(family.position()).add(0, 1f, 0);
            if (projectile.position().dst2(tempCenter) <= familyRadius * familyRadius)
                damageFamily(projectile.owner, family, projectile.baseDamage, projectile.effectIndex);
        }
        for (CharacterRuntime target : players.values())
            if (target.playerId != projectile.owner && target.isAlive()) {
                Vector3 targetCenter = new Vector3(target.position).add(0, 1, 0);
                if (projectile.position().dst2(targetCenter) > explosionRadius2) continue;
                CharacterRuntime owner = player(projectile.owner);
                float critical = criticalMultiplier(owner, projectile.attackId, projectile);
                float attack = attackMultiplier(owner) * (projectile.ultimateAttack() ? ultimateDamageMultiplier(owner) : 1f);
                float dealt = damageService.apply(owner, target, projectile.baseDamage, attack * critical, incomingDamageMultiplier(target), new DamageService.AttackTraits(projectile.ignoresDefense()));
                if (dealt > 0) {
                    target.applyStun(projectile.stunSeconds * passiveProduct(owner, passive -> passive.stunMultiplier(owner)));
                    target.applyBind(projectile.bindSeconds);
                    if (projectile.ultimatePointsOnHit > 0 && pointAwardedAttacks.add(projectile.attackId))
                        addSkillUltimatePoints(owner, projectile.ultimatePointsOnHit);
                    if (critical > 1) {
                        criticalPulse = 1;
                        emitEffect(projectile.position(), 12, 7, .8f);
                    }
                    visualPulse = Math.max(visualPulse, .65f);
                    if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                    if (projectile.hasFollowUp() && target == contactTarget) spawnFollowUp(projectile, target);
                }
            }
        emitEffect(projectile.position(), projectile.effectIndex, explosionRadius * 1.8f, .72f);
        if (projectile.persistentOnHit()) projectile.markHit();
        else projectile.consume();
    }

    private void spawnFollowUp(Projectile source, CharacterRuntime target) {
        Vector3 start = new Vector3(source.position()).add(0, 1.2f, 0);
        Vector3 velocity = new Vector3(target.position).add(0, 1, 0).sub(start).nor().scl(source.followUpSpeed());
        spawn(new Projectile(nextAttackId(), source.owner, source.followUpGlyph(), source.followUpTexture(), source.effectIndex, start, velocity, source.followUpRadius(), source.followUpDamage(), 0, 2.8f));
        emitImageEffect(start, source.followUpTexture(), source.effectIndex, 5.5f, .55f);
    }

    private void spawnSushi(SushiBarrage barrage) {
        CharacterRuntime owner = player(barrage.owner);
        if (owner == null || !owner.isAlive()) return;
        CharacterRuntime target = targetOf(owner.playerId);
        Vector3 aim = target == null ? new Vector3(owner.facing) : new Vector3(target.position).sub(owner.position);
        aim.y = 0f;
        if (aim.isZero()) aim.set(owner.facing);
        aim.nor();
        float spread = (barrage.shotIndex() % 5 - 2) * 3.5f;
        Vector3 direction = aim.rotate(Vector3.Y, spread).nor();
        Vector3 start = new Vector3(owner.position).mulAdd(direction, 1.2f).add(0, 1.1f, 0);
        spawn(new Projectile(nextAttackId(), owner.playerId, "寿司", barrage.texture, barrage.effectIndex, start, direction.scl(barrage.speed), barrage.radius, barrage.damage, 0, barrage.lifetime).attackTraits(true, barrage.ignoreDefense));
        emitImageEffect(start, barrage.texture, barrage.effectIndex, 5.5f, .35f);
    }

    private void resolveLineStrike(LineStrike strike) {
        for (CombatObject object : objects)
            if (object instanceof FamilySummon family && family.owner != strike.owner && !family.expired()) {
                float relativeX = family.position().x - strike.position().x, relativeZ = family.position().z - strike.position().z;
                float along = relativeX * strike.direction.x + relativeZ * strike.direction.z;
                float side = Math.abs(relativeX * strike.direction.z - relativeZ * strike.direction.x);
                if (along >= -family.collisionRadius() && along <= strike.length * 1.3f && side <= strike.width * 2.6f + family.collisionRadius())
                    damageFamily(strike.owner, family, strike.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, strike.impactEffectIndex);
            }
        for (CharacterRuntime target : players.values())
            if (target.playerId != strike.owner && target.isAlive()) {
                float relativeX = target.position.x - strike.position().x, relativeZ = target.position.z - strike.position().z;
                float along = relativeX * strike.direction.x + relativeZ * strike.direction.z;
                float side = Math.abs(relativeX * strike.direction.z - relativeZ * strike.direction.x);
                if (along >= -target.definition.collisionRadius && along <= strike.length * 1.3f && side <= strike.width * 2.6f + target.definition.collisionRadius) {
                    CharacterRuntime owner = player(strike.owner);
                    float critical = criticalMultiplier(owner, System.identityHashCode(strike));
                    float dealt = damageService.apply(owner, target, strike.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (strike.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * critical, incomingDamageMultiplier(target));
                    if (dealt > 0) {
                        visualPulse = 1f;
                        emitEffect(target.position, strike.impactEffectIndex, strike.width * 3.5f, 1f);
                    }
                    if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                }
            }
    }

    private void resolveStrike(UltimateStrike strike) {
        float hitRadius = strike.radius * 2.5f;
        float sourceScale = strike.isMainUltimate() ? GLOBAL_ULTIMATE_DAMAGE_SCALE : 1f;
        for (CombatObject object : objects)
            if (object instanceof FamilySummon family && family.owner != strike.owner && !family.expired()) {
                float familyRadius = hitRadius + family.collisionRadius();
                float familyDx = family.position().x - strike.position().x, familyDz = family.position().z - strike.position().z;
                if (familyDx * familyDx + familyDz * familyDz <= familyRadius * familyRadius)
                    damageFamily(strike.owner, family, strike.baseDamage * sourceScale, strike.impactEffectIndex);
            }
        for (CharacterRuntime target : players.values())
            if (target.playerId != strike.owner && target.isAlive()) {
                float dx = target.position.x - strike.position().x, dz = target.position.z - strike.position().z;
                if (dx * dx + dz * dz <= hitRadius * hitRadius) {
                    CharacterRuntime owner = player(strike.owner);
                    float critical = criticalMultiplier(owner, System.identityHashCode(strike));
                    float ultimateMultiplier = strike.isMainUltimate()
                            ? ultimateDamageMultiplier(owner)
                            : 1f;
                    float dealt = damageService.apply(
                            owner,
                            target,
                            strike.baseDamage * sourceScale,
                            attackMultiplier(owner) * ultimateMultiplier * critical,
                            incomingDamageMultiplier(target));
                    if (dealt > 0) {
                        visualPulse = 1f;
                        emitEffect(strike.position(), strike.impactEffectIndex, strike.radius * 2.1f, 1f);
                    }
                    if (dealt > 0 && strike.burnDuration > 0)
                        target.statuses.apply(new StatusEffect("burn:" + strike.owner, strike.owner, StatusType.BURN, strike.burnDamage, strike.burnDuration).tickEvery(strike.burnTickInterval).ultimatePointsPerTick(strike.burnUltimatePointsPerTick));
                    if (critical > 1) {
                        criticalPulse = 1;
                        emitEffect(strike.position(), 12, strike.radius * 4, 1);
                    }
                    if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                }
            }
    }

    private void resolveCar(CarHazard car) {
        float hitRadius = car.radius * 2f;
        boolean hit = false;
        for (CombatObject object : objects)
            if (object instanceof FamilySummon family && (car.friendlyFire || family.owner != car.owner) && !family.expired()) {
                float combined = hitRadius + family.collisionRadius();
                if (car.position().dst2(family.position()) <= combined * combined) {
                    damageFamily(car.owner, family, car.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, car.effectIndex);
                    hit = true;
                }
            }
        for (CharacterRuntime target : players.values())
            if ((car.friendlyFire || target.playerId != car.owner) && target.isAlive()) {
                float combined = hitRadius + target.definition.collisionRadius;
                if (car.position().dst2(target.position) <= combined * combined) {
                    CharacterRuntime owner = player(car.owner);
                    float dealt = damageService.apply(owner, target, car.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (car.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * criticalMultiplier(owner, System.identityHashCode(car)), incomingDamageMultiplier(target));
                    if (dealt > 0) {
                        target.applyStun(car.stunSeconds);
                        if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                        hit = true;
                    }
                }
            }
        if (hit) {
            car.resetDamagePulse();
            visualPulse = 1f;
            emitEffect(car.position(), car.effectIndex, car.radius * 4f, .65f);
        }
    }

    private void resolveSpinningOne(SpinningOne one) {
        one.direction(tempCenter);
        boolean hit = false;
        for (CombatObject object : objects)
            if (object instanceof FamilySummon family && family.owner != one.owner && !family.expired()) {
                if (insideCenteredLine(one.position(), tempCenter, one.length, one.width * 2f, family.position(), family.collisionRadius())) {
                    damageFamily(one.owner, family, one.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, one.effectIndex);
                    hit = true;
                }
            }
        for (CharacterRuntime target : players.values())
            if (target.playerId != one.owner && target.isAlive()) {
                if (insideCenteredLine(one.position(), tempCenter, one.length, one.width * 2f, target.position, target.definition.collisionRadius)) {
                    CharacterRuntime owner = player(one.owner);
                    float normalDamage = one.baseDamage
                            * GLOBAL_ULTIMATE_DAMAGE_SCALE
                            * attackMultiplier(owner)
                            * ultimateDamageMultiplier(owner)
                            * criticalMultiplier(owner, System.identityHashCode(one));
                    float dealt = damageService.apply(
                            owner,
                            target,
                            normalDamage + one.consumeMissingHealthBonus(target.playerId),
                            1f,
                            incomingDamageMultiplier(target));
                    if (dealt > 0) {
                        if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                        hit = true;
                    }
                }
            }
        if (hit) {
            player(one.owner).heal(one.healOnHit);
            one.resetDamagePulse();
            visualPulse = 1f;
            emitEffect(one.position(), one.effectIndex, one.length * .7f, .48f);
        }
    }

    private static boolean insideCenteredLine(Vector3 center, Vector3 direction, float length, float width, Vector3 target, float targetRadius) {
        float x = target.x - center.x, z = target.z - center.z;
        float along = x * direction.x + z * direction.z;
        float side = Math.abs(x * direction.z - z * direction.x);
        return Math.abs(along) <= length * .5f + targetRadius && side <= width + targetRadius;
    }

    private float attackMultiplier(CharacterRuntime owner) {
        float passive = passiveProduct(owner, value -> value.attackMultiplier(owner, timer.elapsed(), familyCount(owner.playerId)));
        float finale = finalMinuteStarted ? config.finalMinuteAttackMultiplier : 1;
        float status = owner.statuses.multiplier(StatusType.ATTACK_UP) * owner.statuses.multiplier(StatusType.ATTACK_DOWN) * owner.statuses.multiplier(StatusType.BONUS_DAMAGE);
        return passive * finale * status * fieldEffects.multiplier("attack_growth", timer.elapsed()) * (owner.awakened() ? awakeningConfig.attackMultiplier : 1f);
    }

    private float passiveHitboxMultiplier(PlayerId ownerId) {
        CharacterRuntime owner = player(ownerId);
        return passiveProduct(owner, value -> value.hitboxMultiplier(owner));
    }

    private float incomingDamageMultiplier(CharacterRuntime target) {
        float vulnerability = target.statuses.multiplier(StatusType.DAMAGE_TAKEN_UP)
                * fieldEffects.multiplier("damage_taken", timer.elapsed());
        if (target.definition.zeroDefense) {
            return vulnerability;
        }
        float defense = passiveProduct(target, value -> value.incomingDamageMultiplier(target));
        return defense
                * vulnerability
                * (target.awakened() ? 1f / awakeningConfig.defenseMultiplier : 1f);
    }

    private final Vector3 tempCenter = new Vector3();

    private void updateFamily(FamilySummon family, float delta) {
        if (family.expired()) return;
        CharacterRuntime leader = player(family.owner);
        CharacterRuntime target = targetOf(family.owner);
        if (target == null) return;
        tempCenter.set(leader.position).add(MathUtils.cos(family.orbitAngle()) * family.orbitRadius, 0, MathUtils.sin(family.orbitAngle()) * family.orbitRadius);
        tempCenter.x = MathUtils.clamp(tempCenter.x, -config.arenaHalfExtent + 1, config.arenaHalfExtent - 1);
        tempCenter.z = MathUtils.clamp(tempCenter.z, -config.arenaHalfExtent + 1, config.arenaHalfExtent - 1);
        family.position().lerp(tempCenter, Math.min(1, delta * 5f));
        if (!family.attackReady() || !target.isAlive()) return;
        Vector3 start = new Vector3(family.position()).add(0, 1f, 0);
        Vector3 velocity = new Vector3(target.position).add(0, 1f, 0).sub(start).nor().scl(family.projectileSpeed * 1.3f);
        pendingObjects.add(new Projectile(nextAttackId(), family.owner, "A", family.texturePath, family.effectIndex, start, velocity, .58f, family.attackDamage, 0, 2.4f).attackTraits(true, false));
        pendingObjects.add(new VisualEffect(start, family.effectIndex, 2.2f, .28f));
        family.resetAttack();
    }

    private void damageFamily(PlayerId attackerId, FamilySummon family, float baseDamage, int effectIndex) {
        CharacterRuntime attacker = player(attackerId);
        float dealt = family.receiveDamage(baseDamage * attackMultiplier(attacker));
        attacker.recordDamage(dealt);
        if (dealt > 0) {
            emitEffect(family.position(), effectIndex, family.expired() ? 4.8f : 2.8f, family.expired() ? .9f : .4f);
            if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
        }
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId) {
        boolean enabled = owner.definition.criticalEnabled || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP);
        if (!enabled) return 1f;
        float rate = MathUtils.clamp(criticalConfig.baseRate + owner.definition.criticalRate + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate") + fieldEffects.additive("critical_enable"), 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);
        if (roll >= rate * 10000f) return 1f;
        return Math.max(criticalConfig.baseDamageMultiplier, owner.definition.criticalDamageMultiplier + fieldEffects.additive("critical_damage")) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId, Projectile projectile) {
        if (!projectile.hasCriticalProfile()) return criticalMultiplier(owner, attackId);
        boolean enabled = projectile.criticalEnabled() || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP);
        if (!enabled) return 1f;
        float rate = MathUtils.clamp(criticalConfig.baseRate + projectile.criticalRate() + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate") + fieldEffects.additive("critical_enable"), 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);
        return roll < rate * 10000f ? Math.max(criticalConfig.baseDamageMultiplier, projectile.criticalDamageMultiplier() + fieldEffects.additive("critical_damage")) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP) : 1f;
    }

    private void updateStatusDamage(CharacterRuntime target, float delta) {
        for (StatusEffect effect : target.statuses.active())
            if (effect.type == StatusType.BURN || effect.type == StatusType.HEAL_OVER_TIME) {
                effect.tickRemaining -= delta;
                if (effect.tickRemaining <= 0) {
                    effect.tickRemaining += effect.tickInterval;
                    if (effect.type == StatusType.HEAL_OVER_TIME) target.heal(effect.magnitude * effect.stacks);
                    else {
                        CharacterRuntime source = player(effect.source);
                        float dealt = damageService.apply(source, target, effect.magnitude * effect.stacks, 1, incomingDamageMultiplier(target));
                        if (effect.ultimatePointsPerTick > 0)
                            addSkillUltimatePoints(source, effect.ultimatePointsPerTick);
                        if (mode == BattleMode.TRAINING) trainingStatistics.record(dealt);
                        emitEffect(target.position, 4, 4, .6f);
                    }
                }
            }
    }

    private void updateFieldHealing(float delta) {
        if (!fieldEffects.hasType("periodic_heal")) return;
        fieldHealTimer -= delta;
        if (fieldHealTimer > 0) return;
        fieldHealTimer += fieldEffects.interval("periodic_heal", 10f);
        float amount = fieldEffects.additive("periodic_heal");
        for (CharacterRuntime player : players.values()) player.heal(player.definition.maxHp * amount);
    }

    private void updateMatsudoTraffic(float delta) {
        if (matsudoOwners.isEmpty()) return;
        trafficTimer -= delta;
        if (trafficTimer > 0) return;
        trafficTimer += fieldEffects.interval("traffic", 7f);
        int lane = 0;
        for (PlayerId owner : matsudoOwners) {
            for (int index = 0; index < 2; index++) {
                int laneIndex = Math.floorMod(trafficWave + lane++, 4);
                float z = -config.arenaHalfExtent * .65f + laneIndex * config.arenaHalfExtent * .43f;
                boolean eastbound = (trafficWave + index) % 2 == 0;
                Vector3 start = new Vector3(eastbound ? -config.arenaHalfExtent + 1f : config.arenaHalfExtent - 1f, config.groundY, z);
                Vector3 direction = new Vector3(eastbound ? 1 : -1, 0, index == 0 ? .08f : -.08f);
                spawn(new CarHazard(owner, start, direction, 18f, 1.7f, 18f, .35f, 1.6f, config.arenaHalfExtent, "objects/d-racing-car.png", 11, true));
                emitImageEffect(start, "objects/d-racing-car.png", 11, 7f, .8f);
            }
        }
        trafficWave++;
    }

    private void spawnAwakening() {
        awakeningSpawned = true;
        Vector3 position = new Vector3();
        float lowest = Float.MAX_VALUE;
        CharacterRuntime preferred = null;
        boolean tied = false;
        for (CharacterRuntime candidate : players.values()) {
            if (candidate.hpRatio() < lowest - .001f) {
                lowest = candidate.hpRatio();
                preferred = candidate;
                tied = false;
            } else if (Math.abs(candidate.hpRatio() - lowest) < .001f) tied = true;
        }
        position.set(tied || preferred == null ? safeNeutralAwakeningPosition() : safeAwakeningPosition(preferred));
        objects.add(new AwakeningItem(position, awakeningConfig.texturePath, awakeningConfig.pickupRadius));
        emitImageEffect(position, awakeningConfig.texturePath, 15, 7, 1.4f);
    }

    private Vector3 safeAwakeningPosition(CharacterRuntime preferred) {
        float clearance = awakeningConfig.pickupRadius + .9f;
        for (int index = 0; index < 16; index++) {
            float angle = index * MathUtils.PI2 / 16f;
            Vector3 candidate = new Vector3(preferred.position).add(MathUtils.cos(angle) * awakeningConfig.preferredDistance, 0, MathUtils.sin(angle) * awakeningConfig.preferredDistance);
            if (Math.abs(candidate.x) > config.arenaHalfExtent - clearance || Math.abs(candidate.z) > config.arenaHalfExtent - clearance)
                continue;
            boolean clear = true;
            for (CharacterRuntime player : players.values()) {
                float minimum = awakeningConfig.pickupRadius + player.definition.collisionRadius + .6f;
                if (candidate.dst2(player.position) < minimum * minimum) {
                    clear = false;
                    break;
                }
            }
            if (clear) return candidate.set(candidate.x, config.groundY, candidate.z);
        }
        return new Vector3(0, config.groundY, 0);
    }

    private Vector3 safeNeutralAwakeningPosition() {
        for (int ring = 0; ring < 3; ring++)
            for (int index = 0; index < 16; index++) {
                float radius = ring * 2f, angle = index * MathUtils.PI2 / 16f;
                Vector3 candidate = new Vector3(MathUtils.cos(angle) * radius, config.groundY, MathUtils.sin(angle) * radius);
                boolean clear = true;
                for (CharacterRuntime player : players.values()) {
                    float minimum = awakeningConfig.pickupRadius + player.definition.collisionRadius + .6f;
                    if (candidate.dst2(player.position) < minimum * minimum) {
                        clear = false;
                        break;
                    }
                }
                if (clear) return candidate;
            }
        return safeAwakeningPosition(player(PlayerId.ONE));
    }

    private void resolveAwakening(AwakeningItem item) {
        if (item.expired()) return;
        for (CharacterRuntime player : players.values()) {
            float combined = item.radius + player.definition.collisionRadius;
            if (item.position().dst2(player.position) <= combined * combined) {
                player.awaken();
                item.consume();
                emitImageEffect(player.position, awakeningConfig.texturePath, 15, 10, 2);
                break;
            }
        }
    }

    private static boolean contains(String[] values, String target) {
        if (values == null) return false;
        for (String value : values) if (value.equals(target)) return true;
        return false;
    }

    public CharacterRuntime player(PlayerId id) {
        return players.get(id);
    }

    public List<PlayerId> playerIds() {
        return List.copyOf(players.keySet());
    }

    public List<CharacterRuntime> players() {
        return List.copyOf(players.values());
    }

    public CharacterRuntime targetOf(PlayerId owner) {
        CharacterRuntime source = player(owner), best = null;
        float bestDistance = Float.MAX_VALUE;
        for (CharacterRuntime candidate : players.values())
            if (candidate.playerId != owner && candidate.isAlive()) {
                float distance = source.position.dst2(candidate.position);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        return best;
    }

    public PlayerTeam team(PlayerId id) {
        return teams.get(id);
    }

    public SupportCharacterRuntime support(PlayerId id) {
        return team(id).support();
    }

    public List<CombatObject> objects() {
        return List.copyOf(objects);
    }

    public BattleTimer timer() {
        return timer;
    }

    public BattleResult result() {
        return result;
    }

    public BattleMode mode() {
        return mode;
    }

    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean value) {
        paused = value;
    }

    public boolean acceptsActions() {
        return !paused && result == null;
    }

    public boolean finalMinuteStarted() {
        return finalMinuteStarted;
    }

    public void spawn(CombatObject object) {
        if (acceptsActions()) {
            if (object instanceof Projectile projectile && fieldEffects.hasType("projectile_curve"))
                projectile.curve(8f * fieldProjectileCurve());
            pendingObjects.add(object);
        }
    }

    public void emitEffect(Vector3 position, int effectIndex, float scale, float duration) {
        if (acceptsActions())
            pendingObjects.add(new VisualEffect(position, Math.floorMod(effectIndex, 16), scale * 2f, duration));
    }

    public void emitImageEffect(Vector3 position, String texturePath, int effectIndex, float scale, float duration) {
        if (acceptsActions())
            pendingObjects.add(new VisualEffect(position, Math.floorMod(effectIndex, 16), scale * 2f, duration, texturePath));
    }

    public long nextAttackId() {
        return ++attackSequence;
    }

    public void announceUltimate(PlayerId player, String key) {
        announcement = new Announcement(player, key, 2.4f);
    }

    public Announcement announcement() {
        return announcement;
    }

    public TrainingStatistics trainingStatistics() {
        return trainingStatistics;
    }

    public float visualPulse() {
        return visualPulse;
    }

    public float criticalPulse() {
        return criticalPulse;
    }

    public List<String> fieldEffectIds() {
        return fieldEffects.ids();
    }

    /**
     * HUDを戦闘計算の実装へ依存させず、現在の実効値を確認できるスナップショットを返す。
     */
    public CharacterDebugSnapshot debugSnapshot(PlayerId id) {
        CharacterRuntime value = player(id);
        float passiveMove = passiveProduct(
                value,
                passive -> passive.moveSpeedMultiplier(value, timer.elapsed()));
        float statusMove = value.statuses.multiplier(StatusType.MOVE_UP)
                * value.statuses.multiplier(StatusType.MOVE_DOWN);
        float effectiveMoveSpeed = value.definition.moveSpeed
                * 1.2f
                * passiveMove
                * fieldMoveMultiplier()
                * statusMove
                * (value.awakened() ? awakeningConfig.moveSpeedMultiplier : 1f);
        float cooldownMultiplier = config.skillCooldownMultiplier
                * passiveProduct(value, passive -> passive.skillCooldownMultiplier(value))
                * value.statuses.multiplier(StatusType.COOLDOWN_REDUCTION)
                * fieldEffects.multiplier("cooldown", timer.elapsed());
        float ultimateGainMultiplier = passiveProduct(
                value,
                passive -> passive.ultimateGainMultiplier(value))
                * value.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP)
                * (value.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f);
        float criticalRate = MathUtils.clamp(
                criticalConfig.baseRate
                        + value.definition.criticalRate
                        + value.statuses.additive(StatusType.CRITICAL_RATE_UP)
                        + fieldEffects.additive("critical_rate")
                        + fieldEffects.additive("critical_enable"),
                0,
                1);
        float criticalDamage = Math.max(
                criticalConfig.baseDamageMultiplier,
                value.definition.criticalDamageMultiplier
                        + fieldEffects.additive("critical_damage"))
                * value.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);
        return new CharacterDebugSnapshot(
                value.definition.id,
                value.hp(),
                value.definition.maxHp,
                value.missingHp(),
                value.definition.baseAttack,
                attackMultiplier(value),
                value.definition.moveSpeed,
                effectiveMoveSpeed,
                incomingDamageMultiplier(value),
                value.definition.collisionRadius,
                passiveHitboxMultiplier(id),
                value.skillCooldown(),
                value.skillChargeRatio(),
                value.skillStocks(),
                value.maximumSkillStocks(),
                value.ultimatePoints(),
                value.totalDamage(),
                value.stunRemaining(),
                value.movementLocked(),
                value.awakened(),
                value.isAlive(),
                value.definition.zeroDefense,
                value.position.x,
                value.position.y,
                value.position.z,
                value.facing.x,
                value.facing.z,
                cooldownMultiplier,
                ultimateGainMultiplier,
                passiveProduct(value, passive -> passive.stunMultiplier(value)),
                ultimateDamageMultiplier(value),
                criticalRate,
                criticalDamage,
                value.definition.criticalEnabled
                        || fieldEffects.hasType("critical_enable")
                        || value.statuses.has(StatusType.CRITICAL_RATE_UP));
    }

    public float fieldProjectileCurve() {
        return fieldEffects.additive("projectile_curve");
    }

    public float fieldMoveMultiplier() {
        return fieldEffects.multiplier("move_multiplier", timer.elapsed()) * fieldEffects.timedMultiplier("speed_pulse", timer.elapsed());
    }

    public float supportSkillCooldown(float baseSeconds) {
        return baseSeconds * config.skillCooldownMultiplier * fieldEffects.multiplier("cooldown", timer.elapsed());
    }

    public void halveAllSkillCooldowns() {
        for (CharacterRuntime value : players.values()) value.scaleSkillCooldown(.5f);
        for (PlayerTeam team : teams.values()) if (team.support() != null) team.support().scaleSkillCooldown(.5f);
        visualPulse = 1f;
    }

    public float arenaHalfExtent() {
        return config.arenaHalfExtent;
    }

    public float skillCooldown(CharacterRuntime owner, float baseSeconds) {
        return baseSeconds * config.skillCooldownMultiplier * passiveProduct(owner, value -> value.skillCooldownMultiplier(owner)) * owner.statuses.multiplier(StatusType.COOLDOWN_REDUCTION) * fieldEffects.multiplier("cooldown", timer.elapsed());
    }

    public void addSkillUltimatePoints(CharacterRuntime owner, float basePoints) {
        owner.addUltimatePoints(basePoints * passiveProduct(owner, value -> value.ultimateGainMultiplier(owner)) * owner.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP) * (owner.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f));
    }

    public boolean teleportRight(PlayerId owner, float distance) {
        CharacterRuntime user = player(owner), enemy = targetOf(owner);
        if (enemy == null) return false;
        Vector3 right = new Vector3(-enemy.facing.z, 0, enemy.facing.x).nor();
        for (int index = 0; index < 12; index++) {
            int offset = index == 0 ? 0 : (index + 1) / 2 * (index % 2 == 0 ? -1 : 1);
            Vector3 direction = new Vector3(right).rotate(Vector3.Y, offset * 15f);
            Vector3 candidate = new Vector3(enemy.position).mulAdd(direction, distance);
            float margin = user.definition.collisionRadius + .35f;
            if (Math.abs(candidate.x) > config.arenaHalfExtent - margin || Math.abs(candidate.z) > config.arenaHalfExtent - margin)
                continue;
            float minimum = user.definition.collisionRadius + enemy.definition.collisionRadius + .25f;
            if (candidate.dst2(enemy.position) < minimum * minimum) continue;
            user.position.set(candidate.x, config.groundY, candidate.z);
            return true;
        }
        return false;
    }

    public int familyCount(PlayerId owner) {
        return (int) objects.stream().filter(object -> object instanceof FamilySummon family && family.owner == owner && !family.expired()).count();
    }

    public void trainingResetCooldown() {
        if (mode == BattleMode.TRAINING) player(PlayerId.ONE).resetCooldown();
    }

    public void trainingMaxUltimate() {
        if (mode == BattleMode.TRAINING) {
            player(PlayerId.ONE).maximizeUltimate();
            if (support(PlayerId.ONE) != null) support(PlayerId.ONE).maximizeUltimate();
        }
    }

    public void trainingResetStatistics() {
        if (mode == BattleMode.TRAINING) {
            player(PlayerId.ONE).resetDamage();
            trainingStatistics.reset();
        }
    }

    public void trainingSetElapsed(float seconds) {
        if (mode == BattleMode.TRAINING) timer.setElapsed(seconds);
    }

    public void trainingSpawnAwakening() {
        if (mode == BattleMode.TRAINING && !awakeningSpawned) spawnAwakening();
    }

    private List<IPassive> passives(CharacterRuntime owner) {
        if (owner.definition.trainingDummy) return List.of();
        return java.util.Arrays.stream(owner.definition.equippedPassiveIds()).map(abilities::passive).toList();
    }

    private float passiveProduct(CharacterRuntime owner, java.util.function.Function<IPassive, Float> value) {
        float result = 1f;
        for (IPassive passive : passives(owner)) result *= value.apply(passive);
        return result;
    }

    private float ultimateDamageMultiplier(CharacterRuntime owner) {
        return passiveProduct(owner, value -> value.ultimateDamageMultiplier(owner));
    }
}
