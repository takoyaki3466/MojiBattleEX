package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.audio.BattleSoundCue;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.config.FieldEffectDefinition;
import jp.takoa.mojibattle.config.CriticalConfig;
import jp.takoa.mojibattle.config.AwakeningConfig;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityLoadout;
import jp.takoa.mojibattle.domain.ability.IPassive;
import jp.takoa.mojibattle.domain.combat.AwakeningItem;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.CashBundle;
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
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.event.gameplay.AttackEvent;
import jp.takoa.mojibattle.event.gameplay.CharacterSpawnedEvent;
import jp.takoa.mojibattle.event.gameplay.SkillActivatedEvent;
import jp.takoa.mojibattle.event.gameplay.UltimateActivatedEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
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
    private static final float NORMAL_ATTACK_INTERVAL = 1.25f;
    private static final float NORMAL_ATTACK_DAMAGE = 24f;
    private static final float NORMAL_ATTACK_SPEED = 18f;
    private static final float NORMAL_ATTACK_ULTIMATE_POINTS = 4f;

    /**
     * HUDへ表示する必殺技発動者、翻訳キーおよび表示時間。
     *
     * @param player   発動したプレイヤー
     * @param i18nKey  必殺技名の翻訳キー
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
    private final IdentityHashMap<CharacterRuntime, AbilityLoadout> abilityLoadouts = new IdentityHashMap<>();

    private final SupportController supportController;
    private final FieldEffectSystem fieldEffects = new FieldEffectSystem();
    private final Set<Long> pointAwardedAttacks = new HashSet<>();

    private final List<CombatObject> objects = new ArrayList<>();
    private final List<CombatObject> pendingObjects = new ArrayList<>();
    private final List<BattleSoundCue> soundEvents = new ArrayList<>();

    private final BattleTimer timer;
    private final DamageService damageService;
    private final EventBus gameplayEvents;
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

    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<BattleTeamDefinition> teamDefinitions, List<FieldEffectDefinition> availableFieldEffects, Registry<SupportAbilityImplementation> supportAbilityImplementations) {
        this(config, criticalConfig, awakeningConfig, abilities, mode, teamDefinitions, availableFieldEffects, supportAbilityImplementations, new EventBus());
    }

    /**
     * 戦闘状態を構築し、生成・攻撃・ダメージ等を指定ゲームイベントバスへ通知する。
     * 登録ライフサイクル用バスを渡してはならない。
     */
    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<BattleTeamDefinition> teamDefinitions, List<FieldEffectDefinition> availableFieldEffects, Registry<SupportAbilityImplementation> supportAbilityImplementations, EventBus gameplayEvents) {
        this.config = config;
        this.criticalConfig = criticalConfig;
        this.awakeningConfig = awakeningConfig;
        this.abilities = abilities;
        this.gameplayEvents = gameplayEvents;

        damageService = new DamageService(gameplayEvents);
        supportController = new SupportController(supportAbilityImplementations);

        this.mode = mode;

        timer = new BattleTimer(config.durationSeconds);

        if (teamDefinitions.size() < 2 || teamDefinitions.size() > 4) {
            throw new IllegalArgumentException("Battle requires 2-4 complete teams");
        }

        Vector3[] spawns = {new Vector3(-4, config.groundY, -4), new Vector3(4, config.groundY, 4), new Vector3(-4, config.groundY, 4), new Vector3(4, config.groundY, -4)};

        for (int index = 0; index < teamDefinitions.size(); index++) {
            PlayerId id = PlayerId.values()[index];
            BattleTeamDefinition teamDefinition = teamDefinitions.get(index);
            CharacterDefinition definition = teamDefinition.main();
            List<SupportCharacterDefinition> playerSupports = teamDefinition.supports();
            float healthMultiplier = mode == BattleMode.CHAOS ? config.chaosHealthMultiplier : 1f;
            CharacterRuntime runtime = new CharacterRuntime(id, definition, spawns[index], healthMultiplier);

            players.put(id, runtime);

            if (!definition.trainingDummy) {
                abilityLoadouts.put(runtime, abilities.createLoadout(definition));
            }

            gameplayEvents.post(new CharacterSpawnedEvent(runtime));

            List<SupportCharacterRuntime> supportRuntimes = new ArrayList<>();
            List<CharacterRuntime> subMainRuntimes = new ArrayList<>();

            for (int subIndex = 0; subIndex < teamDefinition.subMains().size(); subIndex++) {
                CharacterDefinition subDefinition = teamDefinition.subMains().get(subIndex);
                float side = subIndex == 0 ? -1.8f : 1.8f;
                CharacterRuntime subMain = new CharacterRuntime(id, subDefinition, new Vector3(spawns[index]).add(side, 0, -1.8f), healthMultiplier);
                subMainRuntimes.add(subMain);
                abilityLoadouts.put(subMain, abilities.createLoadout(subDefinition));
                gameplayEvents.post(new CharacterSpawnedEvent(subMain));
            }

            for (int supportIndex = 0; supportIndex < playerSupports.size(); supportIndex++) {
                SupportCharacterDefinition support = playerSupports.get(supportIndex);

                if (support == null) {
                    continue;
                }

                float side = supportIndex == 0 ? -2f : 2f;

                supportRuntimes.add(new SupportCharacterRuntime(id, support, new Vector3(spawns[index]).add(side, 0, 0)));

                if (support.zeroDefenseMain) {
                    runtime.setZeroDefense(true);
                }

                for (FieldEffectDefinition effect : availableFieldEffects) {
                    if (contains(support.fieldPassiveIds, effect.id)) {
                        fieldEffects.activate(effect);

                        if ("matsudo".equals(effect.id)) {
                            matsudoOwners.add(id);
                        }

                    }
                }
            }
            teams.put(id, new PlayerTeam(runtime, subMainRuntimes, supportRuntimes));

            if (!definition.trainingDummy) {
                abilityLoadouts.get(runtime).primarySkill().initialize(runtime, config.skillCooldownMultiplier);
            }
            for (CharacterRuntime subMain : subMainRuntimes) {
                abilityLoadouts.get(subMain).primarySkill().initialize(subMain, config.skillCooldownMultiplier);
            }

        }
        fieldHealTimer = fieldEffects.interval("periodic_heal", 10f);
        trafficTimer = fieldEffects.interval("traffic", 7f);

        if (mode == BattleMode.TRAINING) {
            players.get(PlayerId.TWO).setImmortal(true);
        }
    }

    /**
     * 固定ステップ1回分の戦闘を進める。
     * 移動、衝突補正、照準、Ability、補助キャラ、継続効果、戦場オブジェクト、勝敗判定の順序を固定し、
     * 同じ入力列に対する戦闘結果がフレーム描画順へ依存しないようにする。
     *
     * @param delta    経過秒。呼び出し側が固定ステップへ分割した値
     * @param commands プレイヤーごとの当該ステップ入力
     */
    public void update(float delta, EnumMap<PlayerId, PlayerCommand> commands) {
        if (paused) {
            return;
        }

        if (result != null) {
            visualPulse = Math.max(0, visualPulse - delta * 3.2f);

            for (CombatObject object : objects) {
                if (object instanceof VisualEffect) {
                    object.update(delta);
                }
            }

            objects.removeIf(CombatObject::expired);
            return;
        }

        visualPulse = Math.max(0, visualPulse - delta * 3.2f);
        criticalPulse = Math.max(0, criticalPulse - delta * 2.5f);
        timer.update(delta);

        if (!finalMinuteStarted && timer.remaining() <= config.finalMinuteSeconds) {
            finalMinuteStarted = true;
        }

        trainingStatistics.update(delta);

        for (CharacterRuntime current : allMains()) {
            current.update(delta);

            for (IPassive passive : passives(current)) {
                passive.update(this, current, delta);
            }
        }

        for (CharacterRuntime current : players.values()) {
            if (current.isAlive() && (mode != BattleMode.TRAINING || current.playerId == PlayerId.ONE)) {
                move(current, commands.getOrDefault(current.playerId, PlayerCommand.idle()), delta);
            }
        }

        List<CharacterRuntime> all = new ArrayList<>(players.values());

        for (int a = 0; a < all.size(); a++) {
            for (int b = a + 1; b < all.size(); b++) {
                separatePlayers(all.get(a), all.get(b));
            }
        }
        for (CharacterRuntime current : players.values()) {
            if (current.isAlive()) {
                CharacterRuntime target = targetOf(current.playerId);

                if (target != null) {
                    aimAtOpponent(current, target);
                    fireNormalAttack(current, target);
                }
            }
        }
        for (CharacterRuntime current : players.values()) {
            if (current.isAlive() && (mode != BattleMode.TRAINING || current.playerId == PlayerId.ONE)) {
                activate(current, commands.getOrDefault(current.playerId, PlayerCommand.idle()));
            }
        }
        updateAutonomousMains(delta);
        for (CharacterRuntime current : players.values()) {
            if (current.isAlive() && !team(current.playerId).supports().isEmpty()) {
                CharacterRuntime target = targetOf(current.playerId);

                if (target != null) {
                    List<SupportCharacterRuntime> supports = team(current.playerId).supports();

                    for (int supportIndex = 0; supportIndex < supports.size(); supportIndex++) {
                        supportController.update(this, supports.get(supportIndex), current, target, supportIndex, supports.size(), delta);
                    }
                }
            }
        }
        for (CharacterRuntime current : allMains()) {
            updateStatusDamage(current, delta);
        }
        updateFieldHealing(delta);
        updateMatsudoTraffic(delta);

        if (!awakeningSpawned && timer.remaining() <= awakeningConfig.spawnAtRemainingSeconds) {
            spawnAwakening();
        }
        updateObjects(delta);

        if (mode != BattleMode.TRAINING) {
            result = resultResolver.resolve(players.values(), timer.expired());
        }

        if (result != null) {
            objects.removeIf(object -> !(object instanceof VisualEffect));
        }

        if (announcement != null) {
            float next = announcement.duration - delta;
            announcement = next > 0 ? new Announcement(announcement.player, announcement.i18nKey, next) : null;
        }

    }

    /**
     * 入力を現在の向き基準のワールド移動へ変換し、各種倍率を適用して戦場内へ制限する。
     */
    private void move(CharacterRuntime player, PlayerCommand command, float delta) {
        if (player.movementLocked()) {
            return;
        }

        Vector2 input = command.movement();
        if (input.isZero()) {
            return;
        }

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

    /**
     * 同一フレームで重なった2キャラクターを、両者へ半分ずつ補正して押し戻す。
     */
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

        if (!source.facing.isZero()) {
            source.facing.nor();
        }
    }

    /**
     * 追従メインを代表の後方へ移動させ、使用可能な通常攻撃・スキル・必殺技を自動発動する。
     */
    private void updateAutonomousMains(float delta) {
        for (PlayerTeam team : teams.values()) {
            CharacterRuntime target = targetOf(team.main().playerId);
            if (target == null) {
                continue;
            }
            for (int index = 0; index < team.subMains().size(); index++) {
                CharacterRuntime subMain = team.subMains().get(index);
                followRepresentative(subMain, team.main(), index, delta);
                aimAtOpponent(subMain, target);
                fireNormalAttack(subMain, target);

                AbilityLoadout loadout = abilityLoadouts.get(subMain);
                if (subMain.skillReady() && loadout.primarySkill().tryActivate(this, subMain)) {
                    gameplayEvents.post(new AttackEvent(subMain, subMain.definition.skillId, false));
                    gameplayEvents.post(new SkillActivatedEvent(subMain, subMain.definition.skillId));
                }
                if (loadout.ultimate().tryActivate(this, subMain)) {
                    gameplayEvents.post(new AttackEvent(subMain, subMain.definition.ultimateId, true));
                    gameplayEvents.post(new UltimateActivatedEvent(subMain, subMain.definition.ultimateId));
                }
            }
        }
    }

    /**
     * 追従メインを左右に分け、代表の背後へ滑らかに追従させる。
     */
    private void followRepresentative(CharacterRuntime follower, CharacterRuntime representative, int index, float delta) {
        Vector3 right = new Vector3(-representative.facing.z, 0, representative.facing.x);
        float side = index % 2 == 0 ? -1.8f : 1.8f;
        float row = 2.2f + index / 2 * 1.5f;
        Vector3 desired = new Vector3(representative.position).mulAdd(right, side).mulAdd(representative.facing, -row);
        Vector3 movement = desired.sub(follower.position);
        movement.y = 0;
        float distance = movement.len();
        if (distance > 10f) {
            follower.position.set(desired.x, config.groundY, desired.z);
        } else if (distance > .2f) {
            float speed = follower.definition.moveSpeed * 1.5f * fieldMoveMultiplier();
            follower.position.mulAdd(movement.nor(), Math.min(distance, speed * delta));
        }
        follower.position.y = config.groundY;
    }

    /**
     * 攻撃対象へ自動追尾する、全メイン共通の低威力投射物を一定間隔で発射する。
     */
    private void fireNormalAttack(CharacterRuntime source, CharacterRuntime target) {
        if (!source.isAlive() || source.definition.trainingDummy || source.isStunned() || !source.normalAttackReady()) {
            return;
        }
        Vector3 start = new Vector3(source.position).add(0, 1.05f, 0);
        Vector3 velocity = new Vector3(target.position).add(0, 1f, 0).sub(start).nor().scl(NORMAL_ATTACK_SPEED);
        Projectile projectile = new Projectile(nextAttackId(), source.playerId, source.normalAttackGlyph(), source.normalAttackTexture(), EffectIds.IMPACT, start, velocity, .48f, NORMAL_ATTACK_DAMAGE * Math.max(.1f, source.definition.baseAttack), 0, 0, 2.5f, false, 0, NORMAL_ATTACK_ULTIMATE_POINTS).source(source);
        spawn(projectile);
        emitEffect(start, EffectIds.IMPACT, 2.6f, .3f);
        source.resetNormalAttack(NORMAL_ATTACK_INTERVAL);
    }

    /**
     * 入力されたメインAbilityと補助必殺を発動し、成功したメインAbilityだけイベントを発火する。
     */
    private void activate(CharacterRuntime user, PlayerCommand command) {
        if (user.definition.trainingDummy) {
            return;
        }

        AbilityLoadout loadout = abilityLoadouts.get(user);

        if (command.useSkill() && loadout.primarySkill().tryActivate(this, user)) {
            gameplayEvents.post(new AttackEvent(user, user.definition.skillId, false));
            gameplayEvents.post(new SkillActivatedEvent(user, user.definition.skillId));
        }

        if (command.useUltimate() && loadout.ultimate().tryActivate(this, user)) {
            gameplayEvents.post(new AttackEvent(user, user.definition.ultimateId, true));
            gameplayEvents.post(new UltimateActivatedEvent(user, user.definition.ultimateId));
        }

        PlayerTeam team = team(user.playerId);

        if (command.useSupportUltimate()) {
            for (SupportCharacterRuntime support : team.supports()) {
                supportController.useUltimate(this, team, support);
            }
        }
    }

    /**
     * 戦場オブジェクトを更新し、型ごとの当たり判定を解決した後で生成・破棄を確定する。
     * 更新中のコレクション変更を避けるため、新規オブジェクトは次の段階まで保留する。
     */
    private void updateObjects(float delta) {
        for (CombatObject object : objects) {
            object.update(delta);
            switch (object) {
                case FamilySummon family -> updateFamily(family, delta);
                case Projectile projectile -> resolveProjectile(projectile);
                case UltimateStrike strike when strike.takeDamagePending() -> resolveStrike(strike);
                case LineStrike line when line.takeDamagePending() -> resolveLineStrike(line);
                case CarHazard car when car.damageReady() -> resolveCar(car);

                case SpinningOne one -> {
                    one.position().set(player(one.owner).position);
                    if (one.damageReady()) {
                        resolveSpinningOne(one);
                    }
                }

                case SkillBarrage barrage when barrage.takePulse() ->
                        abilityLoadouts.get(barrage.source).skill(barrage.skillId).activateFromUltimate(this, barrage.source);
                case SushiBarrage barrage when barrage.takeShot() -> spawnSushi(barrage);
                case AwakeningItem item -> resolveAwakening(item);
                default -> {
                }
            }
        }

        objects.removeIf(CombatObject::expired);
        objects.addAll(pendingObjects);

        pendingObjects.clear();
    }

    /**
     * 飛翔体と敵キャラクター・召喚家族の接触を解決する。
     * 貫通数、単一対象への再命中禁止、追撃、状態異常、必殺ポイント加算もここで確定する。
     */
    private void resolveProjectile(Projectile projectile) {
        if (projectile.expired() || !projectile.hitReady()) {
            return;
        }

        boolean contact = false;

        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired()) {
                continue;
            }

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

        for (CharacterRuntime candidate : players.values()) {
            if (candidate.playerId != projectile.owner && candidate.isAlive()) {

                float combined = projectile.radius * 3f * hitboxMultiplier + candidate.definition.collisionRadius * 1.5f;

                if (projectile.position().dst2(new Vector3(candidate.position).add(0, 1, 0)) <= combined * combined) {
                    contact = true;
                    contactTarget = candidate;

                    break;
                }
            }
        }
        if (!contact) {
            return;
        }

        emitSound(BattleSoundCue.IMPACT);

        float explosionRadius = Math.max(3.2f, projectile.radius * 5f) * hitboxMultiplier;
        float explosionRadius2 = explosionRadius * explosionRadius;

        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired()) {
                continue;
            }

            float familyRadius = explosionRadius + family.collisionRadius();
            tempCenter.set(family.position()).add(0, 1f, 0);

            if (projectile.position().dst2(tempCenter) <= familyRadius * familyRadius) {
                damageFamily(projectile.owner, family, projectile.baseDamage, projectile.effectId);
            }
        }
        for (CharacterRuntime target : players.values()) {
            if (target.playerId != projectile.owner && target.isAlive()) {
                Vector3 targetCenter = new Vector3(target.position).add(0, 1, 0);

                if (projectile.position().dst2(targetCenter) > explosionRadius2) {
                    continue;
                }

                CharacterRuntime owner = projectile.source() == null ? player(projectile.owner) : projectile.source();
                float critical = criticalMultiplier(owner, projectile.attackId, projectile);
                float attack = attackMultiplier(owner) * (projectile.ultimateAttack() ? ultimateDamageMultiplier(owner) : 1f);
                float dealt = damageService.apply(owner, target, projectile.baseDamage, attack * critical, incomingDamageMultiplier(target), new DamageService.AttackTraits(projectile.ignoresDefense()));

                if (dealt > 0) {
                    target.applyStun(projectile.stunSeconds * passiveProduct(owner, passive -> passive.stunMultiplier(owner)));
                    target.applyBind(projectile.bindSeconds);

                    if (projectile.ultimatePointsOnHit > 0 && pointAwardedAttacks.add(projectile.attackId)) {
                        addSkillUltimatePoints(owner, projectile.ultimatePointsOnHit);
                    }

                    if (critical > 1) {
                        criticalPulse = 1;
                        emitEffect(projectile.position(), EffectIds.CRITICAL, 7, .8f);
                    }

                    visualPulse = Math.max(visualPulse, .65f);

                    if (mode == BattleMode.TRAINING) {
                        trainingStatistics.record(dealt);
                    }

                    if (projectile.hasFollowUp() && target == contactTarget) {
                        spawnFollowUp(projectile, target);
                    }
                }
            }
        }
        emitEffect(projectile.position(), projectile.effectId, explosionRadius * 1.8f, .72f);

        if (projectile.persistentOnHit()) {
            projectile.markHit();
        } else projectile.consume();
    }

    private void spawnFollowUp(Projectile source, CharacterRuntime target) {
        Vector3 start = new Vector3(source.position()).add(0, 1.2f, 0);
        Vector3 velocity = new Vector3(target.position).add(0, 1, 0).sub(start).nor().scl(source.followUpSpeed());

        spawn(new Projectile(nextAttackId(), source.owner, source.followUpGlyph(), source.followUpTexture(), source.effectId, start, velocity, source.followUpRadius(), source.followUpDamage(), 0, 2.8f).source(source.source()));
        emitImageEffect(start, source.followUpTexture(), source.effectId, 5.5f, .55f);
    }

    private void spawnSushi(SushiBarrage barrage) {
        CharacterRuntime owner = barrage.source;

        if (owner == null || !owner.isAlive()) {
            return;
        }

        CharacterRuntime target = targetOf(owner.playerId);

        Vector3 aim = target == null ? new Vector3(owner.facing) : new Vector3(target.position).sub(owner.position);

        aim.y = 0f;

        if (aim.isZero()) {
            aim.set(owner.facing);
        }
        aim.nor();
        float spread = (barrage.shotIndex() % 5 - 2) * 3.5f;

        Vector3 direction = aim.rotate(Vector3.Y, spread).nor();
        Vector3 start = new Vector3(owner.position).mulAdd(direction, 1.2f).add(0, 1.1f, 0);

        spawn(new Projectile(nextAttackId(), owner.playerId, "寿司", barrage.texture, barrage.effectId, start, direction.scl(barrage.speed), barrage.radius, barrage.damage, 0, barrage.lifetime).attackTraits(true, barrage.ignoreDefense).source(owner));

        emitImageEffect(start, barrage.texture, barrage.effectId, 5.5f, .35f);
    }

    /**
     * 設置された直線攻撃の矩形範囲にいる全敵へ、一度の着弾ダメージを適用する。
     */
    private void resolveLineStrike(LineStrike strike) {
        boolean hit = false;

        for (CombatObject object : objects) {
            if (object instanceof FamilySummon family && family.owner != strike.owner && !family.expired()) {

                float relativeX = family.position().x - strike.position().x, relativeZ = family.position().z - strike.position().z;
                float along = relativeX * strike.direction.x + relativeZ * strike.direction.z;
                float side = Math.abs(relativeX * strike.direction.z - relativeZ * strike.direction.x);

                if (along >= -family.collisionRadius() && along <= strike.length * 1.3f && side <= strike.width * 2.6f + family.collisionRadius()) {
                    damageFamily(strike.owner, family, strike.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, strike.impactEffectId);
                    hit = true;
                }
            }
        }
        for (CharacterRuntime target : players.values()) {
            if (target.playerId != strike.owner && target.isAlive()) {

                float relativeX = target.position.x - strike.position().x, relativeZ = target.position.z - strike.position().z;
                float along = relativeX * strike.direction.x + relativeZ * strike.direction.z;
                float side = Math.abs(relativeX * strike.direction.z - relativeZ * strike.direction.x);

                if (along >= -target.definition.collisionRadius && along <= strike.length * 1.3f && side <= strike.width * 2.6f + target.definition.collisionRadius) {
                    CharacterRuntime owner = player(strike.owner);

                    float critical = criticalMultiplier(owner, System.identityHashCode(strike));
                    float dealt = damageService.apply(owner, target, strike.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (strike.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * critical, incomingDamageMultiplier(target));

                    if (dealt > 0) {
                        hit = true;
                        visualPulse = 1f;
                        emitEffect(target.position, strike.impactEffectId, strike.width * 3.5f, 1f);
                    }

                    if (mode == BattleMode.TRAINING) {
                        trainingStatistics.record(dealt);
                    }
                }
            }
        }

        if (hit) {
            emitSound(BattleSoundCue.IMPACT);
        }
    }

    /**
     * 落下攻撃の円形範囲にいる全敵と敵召喚物へダメージおよび追加効果を適用する。
     */
    private void resolveStrike(UltimateStrike strike) {
        emitSound(BattleSoundCue.EXPLOSION);

        float hitRadius = strike.radius * 2.5f;
        float sourceScale = strike.isMainUltimate() ? GLOBAL_ULTIMATE_DAMAGE_SCALE : 1f;

        for (CombatObject object : objects) {
            if (object instanceof FamilySummon family && family.owner != strike.owner && !family.expired()) {

                float familyRadius = hitRadius + family.collisionRadius();
                float familyDx = family.position().x - strike.position().x, familyDz = family.position().z - strike.position().z;

                if (familyDx * familyDx + familyDz * familyDz <= familyRadius * familyRadius) {
                    damageFamily(strike.owner, family, strike.baseDamage * sourceScale, strike.impactEffectId);
                }
            }
        }
        for (CharacterRuntime target : players.values()) {
            if (target.playerId != strike.owner && target.isAlive()) {

                float dx = target.position.x - strike.position().x, dz = target.position.z - strike.position().z;

                if (dx * dx + dz * dz <= hitRadius * hitRadius) {
                    CharacterRuntime owner = player(strike.owner);

                    float critical = criticalMultiplier(owner, System.identityHashCode(strike));
                    float ultimateMultiplier = strike.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f;

                    float dealt = damageService.apply(owner, target, strike.baseDamage * sourceScale, attackMultiplier(owner) * ultimateMultiplier * critical, incomingDamageMultiplier(target));

                    if (dealt > 0) {
                        visualPulse = 1f;
                        emitEffect(strike.position(), strike.impactEffectId, strike.radius * 2.1f, 1f);
                    }

                    if (dealt > 0 && strike.burnDuration > 0) {
                        target.statuses.apply(new StatusEffect("burn:" + strike.owner, strike.owner, StatusType.BURN, strike.burnDamage, strike.burnDuration).tickEvery(strike.burnTickInterval).ultimatePointsPerTick(strike.burnUltimatePointsPerTick));
                    }

                    if (critical > 1) {
                        criticalPulse = 1;
                        emitEffect(strike.position(), EffectIds.CRITICAL, strike.radius * 4, 1);
                    }

                    if (mode == BattleMode.TRAINING) {
                        trainingStatistics.record(dealt);
                    }
                }
            }
        }
    }

    /**
     * 車両の進行線上にいる敵を判定し、同じ車両からの多重ヒットを抑止してスタンさせる。
     */
    private void resolveCar(CarHazard car) {
        float hitRadius = car.radius * 2f;
        boolean hit = false;

        for (CombatObject object : objects) {
            if (object instanceof FamilySummon family && (car.friendlyFire || family.owner != car.owner) && !family.expired()) {

                float combined = hitRadius + family.collisionRadius();

                if (car.position().dst2(family.position()) <= combined * combined) {
                    damageFamily(car.owner, family, car.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, car.effectId);
                    hit = true;
                }
            }
        }
        for (CharacterRuntime target : players.values()) {
            if ((car.friendlyFire || target.playerId != car.owner) && target.isAlive()) {

                float combined = hitRadius + target.definition.collisionRadius;

                if (car.position().dst2(target.position) <= combined * combined) {
                    CharacterRuntime owner = player(car.owner);
                    float dealt = damageService.apply(owner, target, car.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (car.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * criticalMultiplier(owner, System.identityHashCode(car)), incomingDamageMultiplier(target));

                    if (dealt > 0) {
                        target.applyStun(car.stunSeconds);

                        if (mode == BattleMode.TRAINING) {
                            trainingStatistics.record(dealt);
                        }
                        hit = true;
                    }
                }
            }
        }
        if (hit) {
            emitSound(BattleSoundCue.IMPACT);
            car.resetDamagePulse();

            visualPulse = 1f;
            emitEffect(car.position(), car.effectId, car.radius * 4f, .65f);
        }
    }

    private void resolveSpinningOne(SpinningOne one) {
        one.direction(tempCenter);
        boolean hit = false;

        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == one.owner || family.expired()) {
                continue;
            }

            if (insideCenteredLine(one.position(), tempCenter, one.length, one.width * 2f, family.position(), family.collisionRadius())) {
                damageFamily(one.owner, family, one.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, one.effectId);
                hit = true;
            }
        }
        for (CharacterRuntime target : players.values()) {
            if (target.playerId == one.owner || !target.isAlive()) {
                continue;
            }

            if (insideCenteredLine(one.position(), tempCenter, one.length, one.width * 2f, target.position, target.definition.collisionRadius)) {
                CharacterRuntime owner = player(one.owner);

                float normalDamage = one.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE * attackMultiplier(owner) * ultimateDamageMultiplier(owner) * criticalMultiplier(owner, System.identityHashCode(one));
                float dealt = damageService.apply(owner, target, normalDamage + one.missingHealthBonusPerPulse(), 1f, incomingDamageMultiplier(target));

                if (dealt > 0) {
                    if (mode == BattleMode.TRAINING) {
                        trainingStatistics.record(dealt);
                    }
                    hit = true;
                }
            }
        }
        if (hit) {
            emitSound(BattleSoundCue.IMPACT);
            player(one.owner).heal(one.healOnHit);
            one.resetDamagePulse();

            visualPulse = 1f;
            emitEffect(one.position(), one.effectId, one.length * .7f, .48f);
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
        float vulnerability = target.statuses.multiplier(StatusType.DAMAGE_TAKEN_UP) * target.statuses.multiplier(StatusType.DAMAGE_TAKEN_DOWN) * fieldEffects.multiplier("damage_taken", timer.elapsed());

        if (target.zeroDefense()) {
            return vulnerability;
        }

        float defense = passiveProduct(target, value -> value.incomingDamageMultiplier(target));

        return defense * vulnerability * (target.awakened() ? 1f / awakeningConfig.defenseMultiplier : 1f);
    }

    private final Vector3 tempCenter = new Vector3();

    private void updateFamily(FamilySummon family, float delta) {
        if (family.expired()) {
            return;
        }

        CharacterRuntime leader = player(family.owner);
        CharacterRuntime target = targetOf(family.owner);

        if (target == null) {
            return;
        }

        tempCenter.set(leader.position).add(MathUtils.cos(family.orbitAngle()) * family.orbitRadius, 0, MathUtils.sin(family.orbitAngle()) * family.orbitRadius);
        tempCenter.x = MathUtils.clamp(tempCenter.x, -config.arenaHalfExtent + 1, config.arenaHalfExtent - 1);
        tempCenter.z = MathUtils.clamp(tempCenter.z, -config.arenaHalfExtent + 1, config.arenaHalfExtent - 1);

        family.position().lerp(tempCenter, Math.min(1, delta * 5f));

        if (!family.attackReady() || !target.isAlive()) {
            return;
        }

        Vector3 start = new Vector3(family.position()).add(0, 1f, 0);
        Vector3 velocity = new Vector3(target.position).add(0, 1f, 0).sub(start).nor().scl(family.projectileSpeed * 1.3f);

        pendingObjects.add(new Projectile(nextAttackId(), family.owner, "A", family.texturePath, family.effectId, start, velocity, .58f, family.attackDamage, 0, 2.4f).attackTraits(true, false));
        pendingObjects.add(new VisualEffect(start, family.effectId, 2.2f, .28f));

        emitSound(BattleSoundCue.PROJECTILE);
        family.resetAttack();
    }

    private void damageFamily(PlayerId attackerId, FamilySummon family, float baseDamage, String effectId) {
        CharacterRuntime attacker = player(attackerId);
        float dealt = family.receiveDamage(baseDamage * attackMultiplier(attacker));

        attacker.recordDamage(dealt);

        if (dealt > 0) {
            emitEffect(family.position(), effectId, family.expired() ? 4.8f : 2.8f, family.expired() ? .9f : .4f);

            if (mode == BattleMode.TRAINING) {
                trainingStatistics.record(dealt);
            }
        }
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId) {
        boolean enabled = owner.definition.criticalEnabled || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP);

        if (!enabled) {
            return 1f;
        }

        float rate = MathUtils.clamp(criticalConfig.baseRate + owner.definition.criticalRate + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate") + fieldEffects.additive("critical_enable"), 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);

        if (roll >= rate * 10000f) {
            return 1f;
        }

        return Math.max(criticalConfig.baseDamageMultiplier, owner.definition.criticalDamageMultiplier + fieldEffects.additive("critical_damage")) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId, Projectile projectile) {
        if (!projectile.hasCriticalProfile()) {
            return criticalMultiplier(owner, attackId);
        }

        boolean enabled = projectile.criticalEnabled() || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP);

        if (!enabled) {
            return 1f;
        }

        float rate = MathUtils.clamp(criticalConfig.baseRate + projectile.criticalRate() + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate") + fieldEffects.additive("critical_enable"), 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);

        return roll < rate * 10000f ? Math.max(criticalConfig.baseDamageMultiplier, projectile.criticalDamageMultiplier() + fieldEffects.additive("critical_damage")) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP) : 1f;
    }

    private void updateStatusDamage(CharacterRuntime target, float delta) {
        for (StatusEffect effect : target.statuses.active()) {
            if (effect.type != StatusType.BURN && effect.type != StatusType.HEAL_OVER_TIME) {
                continue;
            }
            effect.tickRemaining -= delta;
            if (!(effect.tickRemaining <= 0)) {
                continue;
            }
            effect.tickRemaining += effect.tickInterval;

            if (effect.type != StatusType.HEAL_OVER_TIME) {
                CharacterRuntime source = player(effect.source);

                float dealt = damageService.apply(source, target, effect.magnitude * effect.stacks, 1, incomingDamageMultiplier(target));

                if (effect.ultimatePointsPerTick > 0) {
                    addSkillUltimatePoints(source, effect.ultimatePointsPerTick);
                }

                if (mode == BattleMode.TRAINING) {
                    trainingStatistics.record(dealt);
                }

                emitSound(BattleSoundCue.IMPACT);
                emitEffect(target.position, EffectIds.FIRE, 4, .6f);

            } else {
                target.heal(effect.magnitude * effect.stacks);

            }
        }
    }

    private void updateFieldHealing(float delta) {
        if (!fieldEffects.hasType("periodic_heal")) {
            return;
        }

        fieldHealTimer -= delta;

        if (fieldHealTimer > 0) {
            return;
        }

        fieldHealTimer += fieldEffects.interval("periodic_heal", 10f);
        float amount = fieldEffects.additive("periodic_heal");

        for (CharacterRuntime player : players.values()) {
            player.heal(player.maxHp() * amount);
        }
    }

    private void updateMatsudoTraffic(float delta) {
        if (matsudoOwners.isEmpty()) {
            return;
        }

        trafficTimer -= delta;
        if (trafficTimer > 0) {
            return;
        }

        trafficTimer += fieldEffects.interval("traffic", 7f);
        int lane = 0;

        for (PlayerId owner : matsudoOwners) {
            for (int index = 0; index < 2; index++) {
                int laneIndex = Math.floorMod(trafficWave + lane++, 4);
                float z = -config.arenaHalfExtent * .65f + laneIndex * config.arenaHalfExtent * .43f;
                boolean eastbound = (trafficWave + index) % 2 == 0;

                Vector3 start = new Vector3(eastbound ? -config.arenaHalfExtent + 1f : config.arenaHalfExtent - 1f, config.groundY, z);
                Vector3 direction = new Vector3(eastbound ? 1 : -1, 0, index == 0 ? .08f : -.08f);

                spawn(new CarHazard(owner, start, direction, 18f, 1.7f, 18f, .35f, 1.6f, config.arenaHalfExtent, "objects/d-racing-car.png", EffectIds.VEHICLE, true));

                emitImageEffect(start, "objects/d-racing-car.png", EffectIds.VEHICLE, 7f, .8f);
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
            } else if (Math.abs(candidate.hpRatio() - lowest) < .001f) {
                tied = true;
            }
        }

        position.set(tied || preferred == null ? safeNeutralAwakeningPosition() : safeAwakeningPosition(preferred));
        objects.add(new AwakeningItem(position, awakeningConfig.texturePath, awakeningConfig.pickupRadius));

        emitImageEffect(position, awakeningConfig.texturePath, EffectIds.FAMILY, 7, 1.4f);
    }

    private Vector3 safeAwakeningPosition(CharacterRuntime preferred) {
        float clearance = awakeningConfig.pickupRadius + .9f;

        for (int index = 0; index < 16; index++) {
            float angle = index * MathUtils.PI2 / 16f;

            Vector3 candidate = new Vector3(preferred.position).add(MathUtils.cos(angle) * awakeningConfig.preferredDistance, 0, MathUtils.sin(angle) * awakeningConfig.preferredDistance);

            if (Math.abs(candidate.x) > config.arenaHalfExtent - clearance || Math.abs(candidate.z) > config.arenaHalfExtent - clearance) {
                continue;
            }

            boolean clear = true;

            for (CharacterRuntime player : players.values()) {
                float minimum = awakeningConfig.pickupRadius + player.definition.collisionRadius + .6f;

                if (candidate.dst2(player.position) < minimum * minimum) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return candidate.set(candidate.x, config.groundY, candidate.z);
            }
        }

        return new Vector3(0, config.groundY, 0);
    }

    private Vector3 safeNeutralAwakeningPosition() {
        for (int ring = 0; ring < 3; ring++) {
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

                if (clear) {
                    return candidate;
                }
            }
        }
        return safeAwakeningPosition(player(PlayerId.ONE));
    }

    private void resolveAwakening(AwakeningItem item) {
        if (item.expired()) {
            return;
        }

        for (CharacterRuntime player : players.values()) {
            float combined = item.radius + player.definition.collisionRadius;

            if (item.position().dst2(player.position) <= combined * combined) {
                player.awaken();
                item.consume();

                emitImageEffect(player.position, awakeningConfig.texturePath, EffectIds.FAMILY, 10, 2);
                break;
            }
        }
    }

    private static boolean contains(String[] values, String target) {
        if (values == null) {
            return false;
        }

        for (String value : values) {
            if (value.equals(target)) {
                return true;
            }
        }

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

    public List<CharacterRuntime> subMains(PlayerId id) {
        return team(id).subMains();
    }

    private List<CharacterRuntime> allMains() {
        List<CharacterRuntime> result = new ArrayList<>();
        for (PlayerTeam team : teams.values()) {
            result.add(team.main());
            result.addAll(team.subMains());
        }
        return result;
    }

    public CharacterRuntime targetOf(PlayerId owner) {
        CharacterRuntime source = player(owner), best = null;
        float bestDistance = Float.MAX_VALUE;

        for (CharacterRuntime candidate : players.values()) {
            if (candidate.playerId != owner && candidate.isAlive()) {
                float distance = source.position.dst2(candidate.position);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
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

    public List<SupportCharacterRuntime> supports(PlayerId id) {
        return team(id).supports();
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

    /**
     * 戦場オブジェクトの生成を予約する。フィールド補正と種類別の効果音通知も同時に設定する。
     * 勝敗確定後または一時停止中の生成要求は破棄する。
     *
     * @param object 次回のオブジェクト確定段階で追加する戦闘オブジェクト
     */
    public void spawn(CombatObject object) {
        if (!acceptsActions()) {
            return;
        }

        if (object instanceof Projectile projectile && fieldEffects.hasType("projectile_curve")) {
            projectile.curve(8f * fieldProjectileCurve());
        }

        pendingObjects.add(object);

        if (object instanceof Projectile) {
            emitSound(BattleSoundCue.PROJECTILE);

        } else if (object instanceof LineStrike || object instanceof SpinningOne) {
            emitSound(BattleSoundCue.SLASH);

        } else if (object instanceof CarHazard) {
            emitSound(BattleSoundCue.VEHICLE);

        } else if (object instanceof FamilySummon) {
            emitSound(BattleSoundCue.SUPPORT);
        }
    }

    /**
     * 戦闘効果音を画面層の再生キューへ追加する。
     */
    public void emitSound(BattleSoundCue cue) {
        if (acceptsActions()) {
            soundEvents.add(cue);
        }
    }

    /**
     * 蓄積した効果音通知を返し、このフレームのキューを空にする。
     */
    public List<BattleSoundCue> drainSoundEvents() {
        List<BattleSoundCue> drained = List.copyOf(soundEvents);
        soundEvents.clear();
        return drained;
    }

    public void emitEffect(Vector3 position, String effectId, float scale, float duration) {
        if (acceptsActions()) {
            pendingObjects.add(new VisualEffect(position, effectId, scale * 2f, duration));
        }
    }

    public void emitImageEffect(Vector3 position, String texturePath, String effectId, float scale, float duration) {
        if (acceptsActions()) {
            pendingObjects.add(new VisualEffect(position, effectId, scale * 2f, duration, texturePath));
        }
    }

    /**
     * 敵の被ダメージを増加させ、所有する札束をすべて範囲爆発へ変換する。
     */
    public void detonateCashBundles(CharacterRuntime owner, float baseDamage, float radius, float warningSeconds, float effectSeconds, float defenseDownMultiplier, float defenseDownDuration, String texturePath, String warningEffectId, String impactEffectId) {
        for (CharacterRuntime target : players.values()) {
            if (target.playerId != owner.playerId && target.isAlive()) {
                target.statuses.apply(new StatusEffect("heisei_bubble_collapse", owner.playerId, StatusType.DAMAGE_TAKEN_UP, defenseDownMultiplier, defenseDownDuration));
            }
        }
        int detonations = 0;

        for (CombatObject object : objects) {
            if (object instanceof CashBundle cash && cash.owner == owner.playerId && !cash.expired()) {
                spawn(new UltimateStrike(owner.playerId, cash.position(), radius, baseDamage, warningSeconds, effectSeconds, 2f, warningEffectId, impactEffectId, "cash", texturePath, 0));
                cash.detonate();
                detonations++;
            }
        }

        if (detonations == 0) {
            CharacterRuntime target = targetOf(owner.playerId);
            Vector3 position = target == null ? owner.position : target.position;
            spawn(new UltimateStrike(owner.playerId, position, radius, baseDamage, warningSeconds, effectSeconds, 2f, warningEffectId, impactEffectId, "cash", texturePath, 0));
        }
    }

    public long nextAttackId() {
        return ++attackSequence;
    }

    public void announceUltimate(PlayerId player, String key) {
        announcement = new Announcement(player, key, 2.4f);
        emitSound(BattleSoundCue.ULTIMATE);
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

        float passiveMove = passiveProduct(value, passive -> passive.moveSpeedMultiplier(value, timer.elapsed()));
        float statusMove = value.statuses.multiplier(StatusType.MOVE_UP) * value.statuses.multiplier(StatusType.MOVE_DOWN);
        float effectiveMoveSpeed = value.definition.moveSpeed * 1.2f * passiveMove * fieldMoveMultiplier() * statusMove * (value.awakened() ? awakeningConfig.moveSpeedMultiplier : 1f);

        float cooldownMultiplier = config.skillCooldownMultiplier * passiveProduct(value, passive -> passive.skillCooldownMultiplier(value)) * value.statuses.multiplier(StatusType.COOLDOWN_REDUCTION) * fieldEffects.multiplier("cooldown", timer.elapsed());
        float ultimateGainMultiplier = passiveProduct(value, passive -> passive.ultimateGainMultiplier(value)) * value.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP) * (value.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f);
        float criticalRate = MathUtils.clamp(criticalConfig.baseRate + value.definition.criticalRate + value.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate") + fieldEffects.additive("critical_enable"), 0, 1);
        float criticalDamage = Math.max(criticalConfig.baseDamageMultiplier, value.definition.criticalDamageMultiplier + fieldEffects.additive("critical_damage")) * value.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);

        return new CharacterDebugSnapshot(value.definition.id, value.hp(), value.maxHp(), value.missingHp(), value.definition.baseAttack, attackMultiplier(value), value.definition.moveSpeed, effectiveMoveSpeed, incomingDamageMultiplier(value), value.definition.collisionRadius, passiveHitboxMultiplier(id), value.skillCooldown(), value.skillChargeRatio(), value.skillStocks(), value.maximumSkillStocks(), value.ultimatePoints(), value.totalDamage(), value.stunRemaining(), value.movementLocked(), value.awakened(), value.isAlive(), value.zeroDefense(), value.position.x, value.position.y, value.position.z, value.facing.x, value.facing.z, cooldownMultiplier, ultimateGainMultiplier, passiveProduct(value, passive -> passive.stunMultiplier(value)), ultimateDamageMultiplier(value), criticalRate, criticalDamage, value.definition.criticalEnabled || fieldEffects.hasType("critical_enable") || value.statuses.has(StatusType.CRITICAL_RATE_UP));
    }

    public float fieldProjectileCurve() {
        return fieldEffects.additive("projectile_curve");
    }

    public float fieldMoveMultiplier() {
        return fieldEffects.multiplier("move_multiplier", timer.elapsed()) * fieldEffects.timedMultiplier("speed_pulse", timer.elapsed()) * fieldEffects.multiplier("express_maki", timer.elapsed());
    }

    public float supportUltimateChargeMultiplier() {
        return fieldEffects.multiplier("express_maki", timer.elapsed());
    }

    public float supportSkillCooldown(float baseSeconds) {
        return baseSeconds * config.skillCooldownMultiplier * fieldEffects.multiplier("cooldown", timer.elapsed());
    }

    /**
     * すべてのメイン・補助キャラクターに残っているスキル待ち時間を半減する。
     * 新たな基準クールタイムは変更せず、現在進行中の待ち時間だけを短縮する。
     */
    public void halveAllSkillCooldowns() {
        for (CharacterRuntime value : allMains()) {
            value.scaleSkillCooldown(.5f);
        }

        for (PlayerTeam team : teams.values()) {
            for (SupportCharacterRuntime support : team.supports()) {
                support.scaleSkillCooldown(.5f);
            }
        }

        visualPulse = 1f;
    }

    public float arenaHalfExtent() {
        return config.arenaHalfExtent;
    }

    /**
     * 基準クールタイムへゲーム設定、パッシブ、状態、フィールドの倍率を合成する。
     *
     * @param owner       スキルを使用するキャラクター
     * @param baseSeconds Ability定義上の基準秒数
     * @return 現在の戦闘状態を反映した待ち時間
     */
    public float skillCooldown(CharacterRuntime owner, float baseSeconds) {
        return baseSeconds * config.skillCooldownMultiplier * passiveProduct(owner, value -> value.skillCooldownMultiplier(owner)) * owner.statuses.multiplier(StatusType.COOLDOWN_REDUCTION) * fieldEffects.multiplier("cooldown", timer.elapsed());
    }

    public void addSkillUltimatePoints(CharacterRuntime owner, float basePoints) {
        owner.addUltimatePoints(basePoints * passiveProduct(owner, value -> value.ultimateGainMultiplier(owner)) * owner.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP) * (owner.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f));
    }

    /**
     * 対象の右側を起点に候補位置を扇状に探索し、戦場内かつ非重複の最初の位置へ転移する。
     *
     * @return 安全な転移先を確保できた場合は {@code true}
     */
    public boolean teleportRight(PlayerId owner, float distance) {
        CharacterRuntime user = player(owner), enemy = targetOf(owner);

        if (enemy == null) {
            return false;
        }

        Vector3 right = new Vector3(-enemy.facing.z, 0, enemy.facing.x).nor();

        for (int index = 0; index < 12; index++) {
            int offset = index == 0 ? 0 : (index + 1) / 2 * (index % 2 == 0 ? -1 : 1);

            Vector3 direction = new Vector3(right).rotate(Vector3.Y, offset * 15f);
            Vector3 candidate = new Vector3(enemy.position).mulAdd(direction, distance);

            float margin = user.definition.collisionRadius + .35f;

            if (Math.abs(candidate.x) > config.arenaHalfExtent - margin || Math.abs(candidate.z) > config.arenaHalfExtent - margin) {
                continue;
            }

            float minimum = user.definition.collisionRadius + enemy.definition.collisionRadius + .25f;

            if (candidate.dst2(enemy.position) < minimum * minimum) {
                continue;
            }

            user.position.set(candidate.x, config.groundY, candidate.z);
            return true;
        }
        return false;
    }

    public int familyCount(PlayerId owner) {
        return (int) objects.stream().filter(object -> object instanceof FamilySummon family && family.owner == owner && !family.expired()).count();
    }

    public void trainingResetCooldown() {
        if (mode == BattleMode.TRAINING) {
            player(PlayerId.ONE).resetCooldown();
        }
    }

    public void trainingMaxUltimate() {
        if (mode != BattleMode.TRAINING) {
            return;
        }

        player(PlayerId.ONE).maximizeUltimate();
        for (SupportCharacterRuntime support : supports(PlayerId.ONE)) {
            support.maximizeUltimate();
        }
    }

    public void trainingResetStatistics() {
        if (mode == BattleMode.TRAINING) {
            player(PlayerId.ONE).resetDamage();
            trainingStatistics.reset();
        }
    }

    public void trainingSetElapsed(float seconds) {
        if (mode == BattleMode.TRAINING) {
            timer.setElapsed(seconds);
        }
    }

    public void trainingSpawnAwakening() {
        if (mode == BattleMode.TRAINING && !awakeningSpawned) {
            spawnAwakening();
        }
    }

    private List<IPassive> passives(CharacterRuntime owner) {
        if (owner.definition.trainingDummy) {
            return List.of();
        }

        AbilityLoadout loadout = abilityLoadouts.get(owner);
        return loadout == null ? List.of() : loadout.passives();
    }

    private float passiveProduct(CharacterRuntime owner, java.util.function.Function<IPassive, Float> value) {
        float result = 1f;

        for (IPassive passive : passives(owner)) {
            result *= value.apply(passive);
        }

        return result;
    }

    private float ultimateDamageMultiplier(CharacterRuntime owner) {
        return passiveProduct(owner, value -> value.ultimateDamageMultiplier(owner));
    }
}
