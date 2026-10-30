package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.ability.support.SupportSplitMemberSpec;
import jp.takoa.mojibattle.audio.BattleSoundCue;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.config.FieldEffectDefinition;
import jp.takoa.mojibattle.config.CriticalConfig;
import jp.takoa.mojibattle.config.AwakeningConfig;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityLoadout;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.ability.context.PassiveUpdateContext;
import jp.takoa.mojibattle.ability.context.DamageEventContext;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatActions;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatantView;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalAbilityContexts;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageResult;
import jp.takoa.mojibattle.battle.action.spec.DelayedStrikeSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileSpec;
import jp.takoa.mojibattle.battle.action.spec.AreaAttackSpec;
import jp.takoa.mojibattle.battle.action.spec.BattlefieldPulseSpec;
import jp.takoa.mojibattle.battle.presentation.CutInSpec;
import jp.takoa.mojibattle.battle.presentation.DomainSpec;
import jp.takoa.mojibattle.battle.modifier.NormalAttackModifier;
import jp.takoa.mojibattle.battle.combatant.CombatantKind;
import jp.takoa.mojibattle.battle.rule.DuelSpec;
import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.ability.support.SupportSplitSpec;
import jp.takoa.mojibattle.domain.combat.AwakeningItem;
import jp.takoa.mojibattle.domain.combat.ActiveDomain;
import jp.takoa.mojibattle.domain.combat.BattlefieldPulse;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.CashBundle;
import jp.takoa.mojibattle.domain.combat.CombatObject;
import jp.takoa.mojibattle.domain.combat.FamilySummon;
import jp.takoa.mojibattle.domain.combat.TemporarySupportSummon;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.domain.combat.ScheduledCombatAction;
import jp.takoa.mojibattle.domain.combat.SkillBarrage;
import jp.takoa.mojibattle.domain.combat.SpinningOne;
import jp.takoa.mojibattle.domain.combat.SushiBarrage;
import jp.takoa.mojibattle.domain.combat.UltimateStrike;
import jp.takoa.mojibattle.domain.combat.VisualEffect;
import jp.takoa.mojibattle.domain.debug.CharacterDebugSnapshot;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusEffectSystem;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.support.FieldEffectSystem;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;
import jp.takoa.mojibattle.domain.support.SupportController;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.registry.impl.SupportPassiveImpl;
import jp.takoa.mojibattle.battle.event.NormalAttackEvent;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.event.gameplay.AttackEvent;
import jp.takoa.mojibattle.event.gameplay.CharacterSpawnedEvent;
import jp.takoa.mojibattle.event.gameplay.SkillActivatedEvent;
import jp.takoa.mojibattle.event.gameplay.UltimateActivatedEvent;
import jp.takoa.mojibattle.event.gameplay.DamageEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;

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
    private final IdentityHashMap<CharacterRuntime, String> combatantInstanceIds = new IdentityHashMap<>();
    private final IdentityHashMap<Object, String> summonInstanceIds = new IdentityHashMap<>();
    private final List<DuelState> duels = new ArrayList<>();
    private final Map<String, TargetLock> normalAttackTargetLocks = new HashMap<>();
    private long summonSequence;

    private final SupportController supportController;
    private final FieldEffectSystem fieldEffects = new FieldEffectSystem();
    private final Set<Long> pointAwardedAttacks = new HashSet<>();

    private final List<CombatObject> objects = new ArrayList<>();
    private final List<CombatObject> pendingObjects = new ArrayList<>();
    private final List<ActiveDomain> domains = new ArrayList<>();
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
    private final List<CutInState> cutIns = new ArrayList<>();
    private boolean cutInsEnabled = true;
    private long cutInRevision;
    private float visualPulse;
    private float criticalPulse;
    private boolean awakeningSpawned;
    private boolean closed;
    private int ultimateTriggeredSkillDepth;
    private float fieldHealTimer = 10f;
    private float trafficTimer = 7f;
    private int trafficWave;


    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<BattleTeamDefinition> teamDefinitions, List<FieldEffectDefinition> availableFieldEffects, Registry<SupportAbilityImpl> supportAbilityImplementations) {
        this(config, criticalConfig, awakeningConfig, abilities, mode, teamDefinitions, availableFieldEffects, supportAbilityImplementations, new EventBus());
    }

    /**
     * 戦闘状態を構築し、生成・攻撃・ダメージ等を指定ゲームイベントバスへ通知する。
     * 登録ライフサイクル用バスを渡してはならない。
     */
    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<BattleTeamDefinition> teamDefinitions, List<FieldEffectDefinition> availableFieldEffects, Registry<SupportAbilityImpl> supportAbilityImplementations, EventBus gameplayEvents) {
        this(config, criticalConfig, awakeningConfig, abilities, mode, teamDefinitions, availableFieldEffects, supportAbilityImplementations, new Registry<>("support passive implementation", SupportPassiveImpl::id), gameplayEvents);
    }

    public BattleController(BattleConfig config, CriticalConfig criticalConfig, AwakeningConfig awakeningConfig, AbilityRegistry abilities, BattleMode mode, List<BattleTeamDefinition> teamDefinitions, List<FieldEffectDefinition> availableFieldEffects, Registry<SupportAbilityImpl> supportAbilityImplementations, Registry<SupportPassiveImpl> supportPassiveImplementations, EventBus gameplayEvents) {
        this.config = config;
        this.criticalConfig = criticalConfig;
        this.awakeningConfig = awakeningConfig;
        this.abilities = abilities;
        this.gameplayEvents = gameplayEvents;

        damageService = new DamageService(gameplayEvents);
        supportController = new SupportController(supportAbilityImplementations, supportPassiveImplementations);

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

            float healthMultiplier = BattleModeRoster.chaosFormation(mode) ? config.chaosHealthMultiplier : 1f;

            CharacterRuntime runtime = new CharacterRuntime(id, definition, spawns[index], healthMultiplier);

            players.put(id, runtime);
            combatantInstanceIds.put(runtime, id.name().toLowerCase() + ":main:0");

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
                combatantInstanceIds.put(subMain, id.name().toLowerCase() + ":main:" + (subIndex + 1));
                abilityLoadouts.put(subMain, abilities.createLoadout(subDefinition));
                gameplayEvents.post(new CharacterSpawnedEvent(subMain));
            }

            for (int supportIndex = 0; supportIndex < playerSupports.size(); supportIndex++) {
                SupportCharacterDefinition support = playerSupports.get(supportIndex);

                if (support == null) {
                    continue;
                }

                float side = supportIndex == 0 ? -2f : 2f;

                supportRuntimes.add(new SupportCharacterRuntime(id, support, new Vector3(spawns[index]).add(side, 0, 0), id.name().toLowerCase() + ":support:" + supportIndex));

                if (support.zeroDefenseMain) {
                    runtime.setZeroDefense(true);
                }

                for (FieldEffectDefinition effect : availableFieldEffects) {
                    if (contains(support.fieldPassiveIds, effect.id)) {
                        fieldEffects.activate(effect, id);

                    }
                }
            }
            teams.put(id, new PlayerTeam(runtime, subMainRuntimes, supportRuntimes));

            if (!definition.trainingDummy) {
                initializeSkill(runtime);
            }
            for (CharacterRuntime subMain : subMainRuntimes) {
                initializeSkill(subMain);
            }

        }
        for (PlayerTeam team : teams.values()) {
            for (SupportCharacterRuntime support : team.supports()) supportController.initialize(this, team, support);
        }
        fieldHealTimer = fieldEffects.interval("periodic_heal", 10f);
        trafficTimer = fieldEffects.interval("traffic", 7f);

        gameplayEvents.subscribe(DamageEvent.class, this::notifyDamagePassives);

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

            objects.removeIf(object -> !(object instanceof VisualEffect) || object.expired());
            pendingObjects.clear();
            return;
        }

        visualPulse = Math.max(0, visualPulse - delta * 3.2f);
        criticalPulse = Math.max(0, criticalPulse - delta * 2.5f);
        timer.update(delta);
        duels.removeIf(duel -> duel.expiresAt <= timer.elapsed());
        normalAttackTargetLocks.values().removeIf(lock -> lock.expiresAt <= timer.elapsed() || !alive(findByInstanceId(lock.targetInstanceId)));
        updatePresentationStates(delta);

        if (!finalMinuteStarted && timer.remaining() <= config.finalMinuteSeconds) {
            finalMinuteStarted = true;
        }

        trainingStatistics.update(delta);

        for (CharacterRuntime current : allMains()) {
            current.update(delta);

            if (!isActiveBattleCombatant(current)) {
                continue;
            }

            for (PassiveRuntime passive : passives(current)) {
                passive.update(new PassiveUpdateContext(new CoreCombatantView(this, current), new CoreCombatActions(this), delta, elapsedSeconds()));
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
            close();
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

        float passiveSpeed = passiveProduct(player, passive -> passive.moveSpeedMultiplier(new CoreCombatantView(this, player)));
        float fieldSpeed = fieldMoveMultiplier(player);
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

                if (subMain.skillReady() && activateSkill(loadout.primarySkill(), subMain)) {
                    notifySkillActivated(subMain);
                    gameplayEvents.post(new AttackEvent(subMain, subMain.definition.skillId, false));
                    gameplayEvents.post(new SkillActivatedEvent(subMain, subMain.definition.skillId));
                }

                if (activateUltimate(loadout.ultimate(), subMain)) {
                    notifyUltimateActivated(subMain);
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
        float row = 2.2f + (float) index / 2 * 1.5f;

        Vector3 desired = new Vector3(representative.position).mulAdd(right, side).mulAdd(representative.facing, -row);
        Vector3 movement = desired.sub(follower.position);

        movement.y = 0;
        float distance = movement.len();

        if (distance > 10f) {
            follower.position.set(desired.x, config.groundY, desired.z);

        } else if (distance > .2f) {
            float speed = follower.definition.moveSpeed * 1.5f * fieldMoveMultiplier(follower);
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

        NormalAttackProfile profile = normalAttackProfile(source);
        notifyNormalAttack(source);

        for (PassiveRuntime passive : passives(source)) {
            passive.onNormalAttack(ExternalAbilityContexts.create(this, source, target));
        }

        Vector3 start = new Vector3(source.position).add(0, 1.05f, 0);
        Vector3 baseVelocity = new Vector3(target.position).add(0, 1f, 0).sub(start).nor().scl(NORMAL_ATTACK_SPEED);

        for (int projectileIndex = 0; projectileIndex < profile.projectileCount; projectileIndex++) {
            float offset = profile.projectileCount == 1 ? 0f : -profile.spreadDegrees * .5f + profile.spreadDegrees * projectileIndex / (profile.projectileCount - 1f);

            for (int hit = 0; hit < profile.hitsPerAttack; hit++) {
                Vector3 velocity = new Vector3(baseVelocity).rotate(Vector3.Y, offset);
                Projectile projectile = new Projectile(nextAttackId(), source.playerId, source.normalAttackGlyph(), profile.texture, profile.effectId, start, velocity, .48f, NORMAL_ATTACK_DAMAGE * Math.max(.1f, source.definition.baseAttack) * profile.damageMultiplier, 0, 0, 2.5f, false, 0, NORMAL_ATTACK_ULTIMATE_POINTS / (profile.projectileCount * profile.hitsPerAttack)).source(source);

                spawn(projectile);
            }
        }

        emitEffect(start, profile.effectId, 2.6f, .3f);
        source.resetNormalAttack(NORMAL_ATTACK_INTERVAL * profile.intervalMultiplier);
    }

    private void notifyNormalAttack(CharacterRuntime attacker) {
        notifyNormalAttack(combatantInstanceIds.get(attacker), attacker.definition.id, attacker.playerId, NormalAttackEvent.AttackerType.MAIN);
    }

    public void notifyNormalAttack(SupportCharacterRuntime attacker) {
        notifyNormalAttack(attacker.instanceId, attacker.definition.id, attacker.owner, NormalAttackEvent.AttackerType.SUPPORT);
    }

    private void notifyNormalAttack(String instanceId, String characterId, PlayerId owner, NormalAttackEvent.AttackerType type) {
        for (PlayerTeam team : teams.values()) {
            NormalAttackEvent event = new NormalAttackEvent(instanceId, ResourceId.parse(characterId), team.main().playerId == owner ? NormalAttackEvent.Team.ALLY : NormalAttackEvent.Team.ENEMY, type);
            for (SupportCharacterRuntime observer : team.supports())
                supportController.normalAttack(this, observer, event);
        }
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        for (PlayerTeam team : teams.values()) {
            team.supports().forEach(support -> supportController.close(this, support));
        }
    }

    /**
     * 入力されたメインAbilityと補助必殺を発動し、成功したメインAbilityだけイベントを発火する。
     */
    private void activate(CharacterRuntime user, PlayerCommand command) {
        if (user.definition.trainingDummy) {
            return;
        }

        AbilityLoadout loadout = abilityLoadouts.get(user);

        if (command.useSkill() && activateSkill(loadout.primarySkill(), user)) {
            notifySkillActivated(user);
            gameplayEvents.post(new AttackEvent(user, user.definition.skillId, false));
            gameplayEvents.post(new SkillActivatedEvent(user, user.definition.skillId));
        }

        if (command.useUltimate() && activateUltimate(loadout.ultimate(), user)) {
            notifyUltimateActivated(user);
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

    private boolean activateSkill(AbilityRegistry.ConfiguredSkill skill, CharacterRuntime user) {
        if (!acceptsActions() || !user.isAlive() || user.isStunned() || !user.skillReady()) {
            return false;
        }

        float healthCost = user.maxHp() * Math.max(0f, skill.healthCostRatio());

        if (healthCost > 0f && user.hp() <= healthCost) {
            return false;
        }

        if (!skill.runtime().tryActivate(ExternalAbilityContexts.create(this, user))) {
            return false;
        }

        if (healthCost > 0f && !user.spendHealth(healthCost)) {
            throw new IllegalStateException("Health changed during Ability activation");
        }

        if (skill.maximumStocks() > 1 && !user.consumeSkillStock()) {
            throw new IllegalStateException("Skill stocks changed during Ability activation");
        }

        if (skill.maximumStocks() <= 1) {
            user.startSkillCooldown(skillCooldown(user, skill.cooldownSeconds()));
        }
        return true;
    }

    private void initializeSkill(CharacterRuntime user) {
        AbilityRegistry.ConfiguredSkill skill = abilityLoadouts.get(user).primarySkill();
        if (skill.maximumStocks() > 1) {
            user.configureSkillStocks(skill.maximumStocks(), skill.initialStocks(), skill.stockRechargeSeconds() * config.skillCooldownMultiplier);
        }
    }

    private boolean activateUltimate(AbilityRegistry.ConfiguredUltimate ultimate, CharacterRuntime user) {
        if (!acceptsActions() || !user.isAlive() || user.isStunned() || user.ultimatePoints() < ultimate.pointCost()) {
            return false;
        }

        long previousCutInRevision = cutInRevision;

        if (!ultimate.runtime().tryActivate(ExternalAbilityContexts.create(this, user))) {
            return false;
        }

        if (!user.spendUltimate(ultimate.pointCost())) {
            throw new IllegalStateException("Ultimate points changed during Ability activation");
        }

        if (cutInRevision == previousCutInRevision) {
            showDefaultUltimateCutIn(user, user.definition.glyphTexture, ultimate.nameKey());
        }

        announceUltimate(user.playerId, ultimate.nameKey());
        return true;
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
                case TemporarySupportSummon temporary -> updateTemporarySupportSummon(temporary, delta);
                case BattlefieldPulse pulse -> resolveBattlefieldPulse(pulse, delta);
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

                case SkillBarrage barrage when barrage.takePulse() -> activateSkillFromUltimate(barrage);
                case SushiBarrage barrage when barrage.takeShot() -> spawnSushi(barrage);
                case AwakeningItem item -> resolveAwakening(item);
                default -> {
                }
            }
        }

        Set<String> recombine = new HashSet<>();
        for (CombatObject object : objects) {
            if (object instanceof TemporarySupportSummon temporary && temporary.expired() && temporary.recombineOnEnd) {
                recombine.add(temporary.sourceSupportInstanceId);
            }
        }
        List<CombatObject> expiredObjects = objects.stream().filter(CombatObject::expired).toList();
        objects.removeAll(expiredObjects);
        expiredObjects.forEach(summonInstanceIds::remove);
        objects.addAll(pendingObjects);

        pendingObjects.clear();
        for (String supportId : recombine) {
            boolean hasSplit = objects.stream().anyMatch(value -> value instanceof TemporarySupportSummon temporary && temporary.sourceSupportInstanceId.equals(supportId) && !temporary.expired());
            if (!hasSplit) {
                Object support = findOwnedByInstanceId(supportId);
                if (support instanceof SupportCharacterRuntime runtime) runtime.hiddenAndDisabled(false);
            }
        }
    }

    /**
     * 必殺技内のスキル再生が、同じ必殺ゲージを自己充填しないよう発動範囲を記録する。
     */
    private void activateSkillFromUltimate(SkillBarrage barrage) {
        ultimateTriggeredSkillDepth++;
        try {
            abilityLoadouts.get(barrage.source).skill(barrage.skillId).runtime().activateFromUltimate(ExternalAbilityContexts.create(this, barrage.source));
        } finally {
            ultimateTriggeredSkillDepth--;
        }
    }

    /**
     * 現在のスキル発動が、通常の必殺ポイント獲得対象かを返す。
     */
    public boolean awardsSkillUltimatePoints() {
        return ultimateTriggeredSkillDepth == 0;
    }

    private Object findOwnedByInstanceId(String id) {
        for (CharacterRuntime main : allMains()) {
            if (id.equals(instanceIdOf(main))) {
                return main;
            }
        }

        for (PlayerTeam team : teams.values()) {
            for (SupportCharacterRuntime support : team.supports()) {
                if (id.equals(support.instanceId)) {
                    return support;
                }
            }
        }

        for (CombatObject object : objects) {
            if (isLivingSummon(object) && id.equals(instanceIdOf(object))) {
                return object;
            }
        }

        for (CombatObject object : pendingObjects) {
            if (isLivingSummon(object) && id.equals(instanceIdOf(object))) {
                return object;
            }
        }
        return null;
    }

    /**
     * 補助を一時召喚群へ置き換える。召喚は次の更新確定前からBattlefieldViewへ現れる。
     */
    public void splitSupport(SupportCharacterRuntime support, SupportSplitSpec spec) {
        if (!containsBattleCombatant(support) || spec == null || support.hiddenAndDisabled()) {
            throw new IllegalArgumentException("Invalid support split");
        }

        Object target = spec.commonTarget() == null ? null : findByInstanceId(spec.commonTarget().instanceId());

        if (target != null && ownerOf(target) == support.owner) {
            throw new IllegalArgumentException("Split target must be an enemy");
        }

        if (spec.hideAndDisableOriginal()) {
            support.hiddenAndDisabled(true);
        }

        int count = spec.members().size();

        for (int index = 0; index < count; index++) {
            SupportSplitMemberSpec member = spec.members().get(index);
            float angle = MathUtils.PI2 * index / count;

            Vector3 start = new Vector3(support.position).add(MathUtils.cos(angle) * 1.2f, 0, MathUtils.sin(angle) * 1.2f);

            TemporarySupportSummon summon = new TemporarySupportSummon(support.owner, support.instanceId, support.instanceId + ":split:" + (++summonSequence), support.definition.id, member.displayGlyph(), member.texture().toString(), start, support.maximumHp(), member.attackDamage(), member.attackIntervalSeconds(), member.moveSpeed(), target == null ? null : instanceIdOf(target), spec.durationSeconds(), spec.recombineOnEnd());
            spawn(summon);
        }
    }

    private void updateTemporarySupportSummon(TemporarySupportSummon summon, float delta) {
        if (summon.expired()) {
            return;
        }

        Object target = lockedNormalAttackTarget(summon);

        if (target == null) {
            target = findByInstanceId(summon.commonTargetInstanceId);
        }

        if (target == null) {
            target = targetOf(summon.owner);
        }

        if (!alive(target)) {
            return;
        }

        Vector3 targetPosition = positionOf(target);
        Vector3 movement = new Vector3(targetPosition).sub(summon.position());

        movement.y = 0;

        if (movement.len2() > 36f && summon.moveSpeed > 0) {
            summon.position().mulAdd(movement.nor(), Math.min(movement.len(), summon.moveSpeed * delta));
        }
        if (!summon.attackReady()) {
            return;
        }

        Vector3 start = new Vector3(summon.position()).add(0, 1, 0);
        Vector3 velocity = new Vector3(targetPosition).add(0, 1, 0).sub(start).nor().scl(16f);

        spawn(new Projectile(nextAttackId(), summon.owner, summon.glyph, summon.texturePath, EffectIds.IMPACT, start, velocity, .4f, summon.attackDamage, 0, 2.5f).sourceInstanceId(summon.instanceId));

        notifyNormalAttack(summon.instanceId, summon.definitionId, summon.owner, NormalAttackEvent.AttackerType.SUMMON);

        summon.resetAttack();
    }

    /**
     * 領域とカットインの表示寿命を、戦闘の固定時間と同じ時計で進める。
     */
    private void updatePresentationStates(float delta) {
        for (ActiveDomain domain : domains) {
            domain.update(delta);
        }

        domains.removeIf(ActiveDomain::expired);

        cutIns.forEach(state -> state.update(delta));
        cutIns.removeIf(CutInState::expired);
    }

    /**
     * 中央吸引を毎フレーム、範囲ダメージを宣言周期ごとに全敵へ適用する。
     */
    private void resolveBattlefieldPulse(BattlefieldPulse pulse, float delta) {
        CharacterRuntime owner = pulse.owner();
        if (!owner.isAlive()) {
            return;
        }
        boolean damageTick = pulse.takeDamageTick();

        for (CharacterRuntime target : enemiesOf(owner)) {
            Vector3 towardCenter = new Vector3(-target.position.x, 0, -target.position.z);
            float distance = towardCenter.len();

            if (distance > .001f) {
                float movement = Math.min(distance, pulse.spec().pullUnitsPerSecond() * delta);
                target.position.mulAdd(towardCenter.scl(1f / distance), movement);
                clampToArena(target);
            }

            if (damageTick) {
                applyAbilityDamage(owner, target, pulse.spec().damagePerTick());
            }
        }

        if (damageTick) {
            emitEffect(pulse.position(), pulse.spec().effectId().toString(), pulse.spec().effectScale(), Math.min(.25f, pulse.spec().tickIntervalSeconds()));
        }
    }

    private void clampToArena(CharacterRuntime target) {
        target.position.x = MathUtils.clamp(target.position.x, -config.arenaHalfExtent, config.arenaHalfExtent);
        target.position.z = MathUtils.clamp(target.position.z, -config.arenaHalfExtent, config.arenaHalfExtent);
        target.position.y = config.groundY;
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
        Object projectileSource = projectile.sourceInstanceId() == null ? (projectile.source() == null ? player(projectile.owner) : projectile.source()) : findByInstanceId(projectile.sourceInstanceId());

        if (projectileSource == null) {
            return;
        }

        for (CombatObject object : objects) {
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired() || !canDamage(projectileSource, family)) {
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

        for (CharacterRuntime candidate : allMains()) {
            if (candidate.playerId != projectile.owner && candidate.isAlive() && canDamage(projectileSource, candidate)) {

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
            if (!(object instanceof FamilySummon family) || family.owner == projectile.owner || family.expired() || !canDamage(projectileSource, family)) {
                continue;
            }

            float familyRadius = explosionRadius + family.collisionRadius();
            tempCenter.set(family.position()).add(0, 1f, 0);

            if (projectile.position().dst2(tempCenter) <= familyRadius * familyRadius) {
                damageFamily(projectile.owner, family, projectile.baseDamage, projectile.effectId);
            }
        }
        for (CharacterRuntime target : allMains()) {
            if (target.playerId != projectile.owner && target.isAlive() && canDamage(projectileSource, target)) {
                Vector3 targetCenter = new Vector3(target.position).add(0, 1, 0);

                if (projectile.position().dst2(targetCenter) > explosionRadius2) {
                    continue;
                }

                CharacterRuntime owner = projectile.source() == null ? player(projectile.owner) : projectile.source();
                float critical = criticalMultiplier(owner, projectile.attackId, projectile);
                float attack = attackMultiplier(owner) * (projectile.ultimateAttack() ? ultimateDamageMultiplier(owner) : 1f);

                boolean ignoreDefense = projectile.ignoresDefense() || projectile.ignoresDefenseOnCritical() && critical > 1f || passiveIgnoresDefense(owner);

                float dealt = damageService.apply(owner, target, projectile.baseDamage, attack * critical, combatantDefenseMultiplier(target), incomingEffectMultiplier(target) * duelDamageMultiplier(projectileSource, target), new DamageService.AttackTraits(ignoreDefense));

                if (!(dealt > 0)) {
                    continue;
                }

                target.applyStun(projectile.stunSeconds * passiveProduct(owner, passive -> passive.stunMultiplier(view(owner))));
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
                    float dealt = applyLegacyDamage(owner, target, strike.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (strike.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * critical);

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

                    float dealt = applyLegacyDamage(owner, target, strike.baseDamage * sourceScale, attackMultiplier(owner) * ultimateMultiplier * critical);

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
                    float dealt = applyLegacyDamage(owner, target, car.baseDamage * GLOBAL_ULTIMATE_DAMAGE_SCALE, attackMultiplier(owner) * (car.isMainUltimate() ? ultimateDamageMultiplier(owner) : 1f) * criticalMultiplier(owner, System.identityHashCode(car)));

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
                float dealt = applyLegacyDamage(owner, target, normalDamage + one.missingHealthBonusPerPulse(), 1f);

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
        float passive = passiveProduct(owner, value -> value.attackMultiplier(new CoreCombatantView(this, owner)));
        float finale = finalMinuteStarted ? config.finalMinuteAttackMultiplier : 1;
        float status = owner.statuses.multiplier(StatusType.ATTACK_UP) * owner.statuses.multiplier(StatusType.ATTACK_DOWN) * owner.statuses.multiplier(StatusType.BONUS_DAMAGE);

        return passive * finale * status * fieldEffects.multiplier("attack_growth", timer.elapsed(), owner) * fieldEffects.multiplier("attack_multiplier", timer.elapsed(), owner) * (owner.awakened() ? awakeningConfig.attackMultiplier : 1f);
    }

    private float passiveHitboxMultiplier(PlayerId ownerId) {
        CharacterRuntime owner = player(ownerId);

        return passiveProduct(owner, value -> value.hitboxMultiplier(view(owner)));
    }

    private float incomingDamageMultiplier(CharacterRuntime target) {
        return defenseMultiplier(target) * incomingEffectMultiplier(target);
    }

    /**
     * 既存戦闘オブジェクトにも、APIパッシブの防御無視を同一規則で適用する。
     */
    private float applyLegacyDamage(CharacterRuntime owner, CharacterRuntime target, float baseDamage, float attack) {
        if (!canDamage(owner, target)) {
            return 0;
        }
        return damageService.apply(owner, target, baseDamage, attack, combatantDefenseMultiplier(target), incomingEffectMultiplier(target) * duelDamageMultiplier(owner, target), new DamageService.AttackTraits(passiveIgnoresDefense(owner)));
    }

    private float incomingEffectMultiplier(CharacterRuntime target) {
        return target.statuses.multiplier(StatusType.DAMAGE_TAKEN_UP) * target.statuses.multiplier(StatusType.DAMAGE_TAKEN_DOWN) * fieldEffects.multiplier("damage_taken", timer.elapsed(), target) * fieldEffects.multiplier("incoming_damage_multiplier", timer.elapsed(), target);
    }

    private float defenseMultiplier(CharacterRuntime target) {
        if (target.zeroDefense()) {
            return 1f;
        }

        float defense = passiveProduct(target, value -> value.incomingDamageMultiplier(view(target)));

        return defense * (target.awakened() ? 1f / awakeningConfig.defenseMultiplier : 1f);
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
        if (!canDamage(attacker, family)) return;
        float dealt = family.receiveDamage(baseDamage * attackMultiplier(attacker) * duelDamageMultiplier(attacker, family));

        attacker.recordDamage(dealt);

        if (dealt > 0) {
            emitEffect(family.position(), effectId, family.expired() ? 4.8f : 2.8f, family.expired() ? .9f : .4f);

            if (mode == BattleMode.TRAINING) {
                trainingStatistics.record(dealt);
            }
        }
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId) {
        float passiveRate = passiveCriticalRateModifier(owner);
        boolean enabled = owner.definition.criticalEnabled || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP) || passiveRate != 0f;

        if (!enabled) {
            return 1f;
        }

        float rate = MathUtils.clamp(criticalConfig.baseRate + owner.definition.criticalRate + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate", owner) + fieldEffects.additive("critical_enable", owner) + passiveRate, 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);

        if (roll >= rate * 10000f) {
            return 1f;
        }

        return Math.max(criticalConfig.baseDamageMultiplier, owner.definition.criticalDamageMultiplier + fieldEffects.additive("critical_damage", owner)) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);
    }

    private float criticalMultiplier(CharacterRuntime owner, long attackId, Projectile projectile) {
        if (!projectile.hasCriticalProfile()) {
            return criticalMultiplier(owner, attackId);
        }

        float passiveRate = passiveCriticalRateModifier(owner);
        boolean enabled = projectile.criticalEnabled() || fieldEffects.hasType("critical_enable") || owner.statuses.has(StatusType.CRITICAL_RATE_UP) || passiveRate != 0f;

        if (!enabled) {
            return 1f;
        }

        float rate = MathUtils.clamp(criticalConfig.baseRate + projectile.criticalRate() + owner.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate", owner) + fieldEffects.additive("critical_enable", owner) + passiveRate, 0, 1);
        long roll = Math.floorMod(attackId * 1103515245L + owner.playerId.ordinal() * 12345L, 10000L);

        return roll < rate * 10000f ? Math.max(criticalConfig.baseDamageMultiplier, projectile.criticalDamageMultiplier() + fieldEffects.additive("critical_damage", owner)) * owner.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP) : 1f;
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

                float dealt = applyLegacyDamage(source, target, effect.magnitude * effect.stacks, 1f);

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
        Set<PlayerId> trafficOwners = fieldEffects.ownersOfType("traffic");
        if (trafficOwners.isEmpty()) {
            return;
        }

        trafficTimer -= delta;
        if (trafficTimer > 0) {
            return;
        }

        trafficTimer += fieldEffects.interval("traffic", 7f);
        int lane = 0;

        for (PlayerId owner : trafficOwners) {
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

    /**
     * 公開Abilityビューがこの試合に属するメインキャラクターかを同一性で確認する。
     */
    public boolean containsCombatant(CharacterRuntime runtime) {
        return runtime != null && allMains().stream().anyMatch(candidate -> candidate == runtime);
    }

    /**
     * 同じPlayerTeamを除外し、生存中の代表・追従メインを登録順で返す。
     */
    public List<CharacterRuntime> enemiesOf(CharacterRuntime source) {
        if (!containsCombatant(source)) {
            throw new IllegalArgumentException("Source combatant does not belong to this battle");
        }
        return allMains().stream().filter(candidate -> candidate.playerId != source.playerId && candidate.isAlive()).toList();
    }

    /**
     * 使用者自身を含む同じプレイヤーチームの生存メインを登録順で返す。
     */
    public List<CharacterRuntime> alliesOf(CharacterRuntime source) {
        if (!containsCombatant(source)) {
            throw new IllegalArgumentException("Source combatant does not belong to this battle");
        }
        return allMains().stream().filter(candidate -> candidate.playerId == source.playerId && candidate.isAlive()).toList();
    }

    /**
     * 外部Abilityの基礎ダメージを既存攻撃と同じ倍率・防御・会心・統計・イベント経路へ流す。
     */
    public float applyAbilityDamage(CharacterRuntime source, CharacterRuntime target, float baseDamage, DamageType type) {
        return applyAbilityDamage(source, target, DamageRequest.of(baseDamage, type)).dealtDamage();
    }

    /**
     * 構造化ダメージを倍率、会心、防御、統計、イベントの共通経路へ流す。
     */
    public DamageResult applyAbilityDamage(CharacterRuntime source, CharacterRuntime target, DamageRequest request) {
        if (!containsCombatant(source) || !containsCombatant(target)) {
            throw new IllegalArgumentException("Ability combatants must belong to this battle");
        }

        if (request == null) {
            throw new IllegalArgumentException("Damage request is required");
        }

        if (!canDamage(source, target)) {
            return new DamageResult(0, false, request.ignoreDefense());
        }

        float typeScale = switch (request.type()) {
            case NORMAL -> 1f;
            case SKILL -> .55f;
            case ULTIMATE -> GLOBAL_ULTIMATE_DAMAGE_SCALE;
        };

        float ultimate = request.type() == DamageType.ULTIMATE ? ultimateDamageMultiplier(source) : 1f;
        float critical = criticalMultiplier(source, nextAttackId());

        boolean criticalHit = critical > 1f;
        boolean ignoreDefense = request.ignoreDefense() || request.ignoreDefenseOnCritical() && criticalHit || passiveIgnoresDefense(source);

        float dealt = damageService.apply(source, target, request.baseDamage() * typeScale, attackMultiplier(source) * ultimate * critical, combatantDefenseMultiplier(target), incomingEffectMultiplier(target) * duelDamageMultiplier(source, target), new DamageService.AttackTraits(ignoreDefense));

        if (critical > 1f && dealt > 0) {
            criticalPulse = 1f;
            emitEffect(target.position, EffectIds.CRITICAL, 7f, .8f);
        }

        if (mode == BattleMode.TRAINING) {
            trainingStatistics.record(dealt);
        }

        return new DamageResult(dealt, criticalHit, ignoreDefense);
    }

    /**
     * 任意種別の公開戦闘個体へ構造化ダメージを適用する。
     */
    public DamageResult applyCombatantDamage(Object source, Object target, DamageRequest request) {
        if (!containsBattleCombatant(source) || !containsBattleCombatant(target) || request == null) {
            throw new IllegalArgumentException("Damage combatants must belong to this battle");
        }

        if (source instanceof CharacterRuntime sourceMain && target instanceof CharacterRuntime targetMain) {
            return applyAbilityDamage(sourceMain, targetMain, request);
        }
        if (!canDamage(source, target) || !alive(target)) {
            return new DamageResult(0, false, request.ignoreDefense());
        }

        float scale = damageTypeScale(request.type());
        float defense = request.ignoreDefense() ? 1f : combatantDefenseMultiplier(target);
        float incoming = target instanceof FamilySummon ? 1f : statusesOf(target).multiplier(StatusType.DAMAGE_TAKEN_UP) * statusesOf(target).multiplier(StatusType.DAMAGE_TAKEN_DOWN);
        float amount = request.baseDamage() * scale * defense * incoming * duelDamageMultiplier(source, target);
        float dealt;

        switch (target) {
            case SupportCharacterRuntime support -> dealt = support.receiveDamage(amount);

            case FamilySummon family -> {
                float rawBeforeFamilyDefense = request.baseDamage() * scale * duelDamageMultiplier(source, target);
                dealt = family.receiveDamage(rawBeforeFamilyDefense);
            }

            case TemporarySupportSummon temporary -> dealt = temporary.receiveDamage(amount);
            case CharacterRuntime main -> dealt = main.receiveDamage(amount);

            default -> throw new IllegalArgumentException("Unknown combatant");
        }

        if (source instanceof CharacterRuntime main) {
            main.recordDamage(dealt);
        }

        return new DamageResult(dealt, false, request.ignoreDefense());
    }

    public void healCombatant(Object target, float amount) {
        if (!containsBattleCombatant(target) || !Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Invalid heal");
        }

        switch (target) {
            case CharacterRuntime main -> main.heal(amount);
            case SupportCharacterRuntime support -> support.heal(amount);
            case FamilySummon family -> family.heal(amount);
            case TemporarySupportSummon temporary -> temporary.heal(amount);
            case null, default -> {
            }
        }
    }

    /**
     * 現在この戦闘に存在し、生存していて状態管理を持つ個体かを返す。
     */
    public boolean canApplyStatusTo(Object target) {
        return containsBattleCombatant(target) && alive(target) && (target instanceof CharacterRuntime || target instanceof SupportCharacterRuntime || target instanceof TemporarySupportSummon);
    }

    /**
     * 状態付与可能な戦闘個体の状態管理を返す。
     * 所属外・破棄済み・死亡済みと、仕様上状態非対応の個体は区別して拒否する。
     */
    public StatusEffectSystem statusesOf(Object target) {
        if (!containsBattleCombatant(target)) {
            throw new IllegalArgumentException("Combatant does not belong to this battle or is no longer active");
        }

        if (!alive(target)) {
            throw new IllegalArgumentException("Status target must be alive");
        }

        return switch (target) {
            case CharacterRuntime main -> main.statuses;
            case SupportCharacterRuntime support -> support.statuses;
            case TemporarySupportSummon temporary -> temporary.statuses;

            default -> throw new IllegalArgumentException("This combatant does not support status effects");
        };
    }

    /**
     * 視覚だけを担う領域を追加する。同時発動中の領域は解除しない。
     */
    public void activateDomain(Object source, DomainSpec spec) {
        if (!containsBattleCombatant(source) || spec == null) {
            throw new IllegalArgumentException("Domain source and specification must belong to this battle");
        }

        domains.add(new ActiveDomain(source, spec));

        if (spec.ambientEffectId() != null) {
            emitEffect(positionOf(source), spec.ambientEffectId().toString(), 12f * spec.intensity(), .8f);
        }
    }

    /**
     * 指定キャラクター自身が発動した領域だけを照会する。
     */
    public boolean isDomainActive(Object source, ResourceId domainId) {
        if (!containsBattleCombatant(source) || domainId == null) {
            return false;
        }

        return domains.stream().anyMatch(domain -> domain.owner() == source && domain.spec().id().equals(domainId));
    }

    /**
     * 同じ発動元・表示文の旧カットインだけを優先度に従って置換し、別発動は同時表示する。
     */
    public void showCutIn(CharacterRuntime source, CutInSpec spec) {
        if (!containsCombatant(source) || spec == null) {
            throw new IllegalArgumentException("Cut-in source and specification must belong to this battle");
        }

        if (!cutInsEnabled) {
            return;
        }

        for (int index = 0; index < cutIns.size(); index++) {
            CutInState current = cutIns.get(index);
            if (current.source() != source || !current.spec().titleTranslationKey().equals(spec.titleTranslationKey())) {
                continue;
            }

            if (spec.priority() >= current.spec().priority()) {
                cutIns.set(index, new CutInState(source, spec));
                cutInRevision++;
            }

            return;
        }
        cutIns.add(new CutInState(source, spec));
        cutInRevision++;
    }

    /**
     * 個別カットインを持たない必殺技へ、キャラクター画像を使った標準演出を追加する。
     * 名前空間なしの旧形式パスは、他の画像読込みと同じく標準コンテンツのリソースとして解釈する。
     */
    public void showDefaultUltimateCutIn(CharacterRuntime source, String imagePath, String titleTranslationKey) {
        if (imagePath == null || imagePath.isBlank() || titleTranslationKey == null || titleTranslationKey.isBlank()) {
            return;
        }

        ResourceId image = ResourceId.parse(imagePath);
        showCutIn(source, new CutInSpec(image, titleTranslationKey, 2.2f, 10));
    }

    public void setCutInsEnabled(boolean enabled) {
        cutInsEnabled = enabled;
        if (!enabled) {
            cutIns.clear();
        }
    }

    public boolean cutInsEnabled() {
        return cutInsEnabled;
    }

    public long cutInRevision() {
        return cutInRevision;
    }

    /**
     * 中央吸引と周期攻撃を、通常の戦場オブジェクトとして開始する。
     */
    public void startBattlefieldPulse(CharacterRuntime source, BattlefieldPulseSpec spec) {
        if (!containsCombatant(source) || spec == null) {
            throw new IllegalArgumentException("Battlefield pulse source and specification must belong to this battle");
        }

        spawn(new BattlefieldPulse(source, spec));
    }

    /**
     * 2体の位置を交換し、戦場外や高さのずれを補正する。
     */
    public void swapPositions(Object first, Object second) {
        if (!containsBattleCombatant(first) || !containsBattleCombatant(second)) {
            throw new IllegalArgumentException("Position swap combatants must belong to this battle");
        }

        Vector3 firstPosition = positionOf(first);
        Vector3 secondPosition = positionOf(second);
        Vector3 previous = new Vector3(firstPosition);

        firstPosition.set(secondPosition);
        secondPosition.set(previous);

        clampBattleCombatantToArena(first);
        clampBattleCombatantToArena(second);
    }

    /**
     * 対象を戦場中央に対して点対称の位置へ移動する。
     */
    public void mirrorAcrossArenaCenter(Object target) {
        if (!containsBattleCombatant(target)) {
            throw new IllegalArgumentException("Mirrored combatant must belong to this battle");
        }

        Vector3 position = positionOf(target);
        position.set(-position.x, config.groundY, -position.z);
        clampBattleCombatantToArena(target);
    }

    public List<ActiveDomain> domains() {
        return List.copyOf(domains);
    }

    public CutInState cutIn() {
        return cutIns.stream().max(Comparator.comparingInt(state -> state.spec().priority())).orElse(null);
    }

    public List<CutInState> cutIns() {
        return List.copyOf(cutIns);
    }

    /**
     * 獲得倍率を適用する通常の必殺ポイント加算。
     */
    public void addExternalUltimatePoints(CharacterRuntime target, float points) {
        requireOwnedFiniteAmount(target, points, "Ultimate points");

        if (awardsSkillUltimatePoints()) {
            addSkillUltimatePoints(target, points);
        }
    }

    /**
     * 消費取り消し用。獲得倍率を再適用せずゲージ上限へクランプする。
     */
    public void refundUltimatePoints(CharacterRuntime target, float points) {
        requireOwnedFiniteAmount(target, points, "Ultimate point refund");
        target.addUltimatePoints(points);
    }

    /**
     * 必殺ゲージを持つメイン・補助へ、各Runtimeの通常獲得規則でポイントを加算する。
     */
    public void addCombatantUltimatePoints(Object target, float points) {
        requireActiveFiniteAmount(target, points, "Ultimate points");
        if (target instanceof CharacterRuntime main) {
            addExternalUltimatePoints(main, points);

        } else if (target instanceof SupportCharacterRuntime support) {
            support.addUltimatePoints(points);

        } else {
            throw new IllegalArgumentException("This combatant does not have ultimate points");
        }
    }

    /**
     * 必殺ゲージを持つメイン・補助へ、獲得倍率なしでポイントを返す。
     */
    public void refundCombatantUltimatePoints(Object target, float points) {
        requireActiveFiniteAmount(target, points, "Ultimate point refund");
        if (target instanceof CharacterRuntime main) {
            refundUltimatePoints(main, points);

        } else if (target instanceof SupportCharacterRuntime support) {
            support.addUltimatePoints(points);

        } else {
            throw new IllegalArgumentException("This combatant does not have ultimate points");
        }
    }

    private void requireActiveFiniteAmount(Object target, float amount, String name) {
        if (!isActiveBattleCombatant(target)) {
            throw new IllegalArgumentException("Combatant does not belong to this battle or is no longer active");
        }

        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    /**
     * 対象を追跡する固定ステップ遅延攻撃を生成する。
     */
    public void spawnDelayedStrike(CharacterRuntime source, CharacterRuntime target, DelayedStrikeSpec spec) {
        if (!containsCombatant(source) || !containsCombatant(target)) {
            throw new IllegalArgumentException("Delayed strike combatants must belong to this battle");
        }

        if (spec == null) {
            throw new IllegalArgumentException("Delayed strike specification is required");
        }

        if (spec.objectTexture() != null) {
            emitImageEffect(target.position, spec.objectTexture().toString(), spec.effectId() == null ? EffectIds.IMPACT : spec.effectId().toString(), Math.max(.01f, spec.effectScale()), Math.max(.01f, spec.delaySeconds()));
        }

        spawn(new ScheduledCombatAction(target.position, spec.delaySeconds(), () -> {
            if (!acceptsActions() || !source.isAlive() || !target.isAlive()) {
                return;
            }

            applyAbilityDamage(source, target, spec.damage());

            if (spec.effectId() != null) {
                emitEffect(target.position, spec.effectId().toString(), spec.effectScale(), .35f);
            }
        }));
    }

    public void spawnExternalProjectile(CharacterRuntime source, Object target, ProjectileSpec spec) {
        if (!containsCombatant(source) || !containsBattleCombatant(target)) {
            throw new IllegalArgumentException("Projectile combatants must belong to this battle");
        }

        Vector3 start = new Vector3(source.position).add(0, 1.05f, 0);
        Vector3 baseVelocity = new Vector3(positionOf(target)).add(0, 1f, 0).sub(start).nor().scl(spec.speed());

        float typeScale = damageTypeScale(spec.damage().type());

        for (int index = 0; index < spec.projectileCount(); index++) {
            float offset = spec.projectileCount() == 1 ? 0f : -spec.spreadDegrees() * .5f + spec.spreadDegrees() * index / (spec.projectileCount() - 1f);

            for (int hit = 0; hit < spec.hitsPerProjectile(); hit++) {
                Projectile projectile = new Projectile(nextAttackId(), source.playerId, source.normalAttackGlyph(), spec.textureId() == null ? source.normalAttackTexture() : spec.textureId().toString(), spec.effectId() == null ? EffectIds.IMPACT : spec.effectId().toString(), start, new Vector3(baseVelocity).rotate(Vector3.Y, offset), spec.radius(), spec.damage().baseDamage() * typeScale, 0, 0, spec.lifetimeSeconds(), false, 0, 0).source(source).attackTraits(spec.damage().type() == DamageType.ULTIMATE, spec.damage().ignoreDefense(), spec.damage().ignoreDefenseOnCritical());
                spawn(projectile);
            }
        }
    }

    public void spawnAreaAttack(Object source, Object target, AreaAttackSpec spec) {
        if (!containsBattleCombatant(source) || !containsBattleCombatant(target)) {
            throw new IllegalArgumentException("Area attack combatants must belong to this battle");
        }

        Vector3 center = new Vector3(positionOf(target));

        if (spec.objectTexture() != null) {
            String effectId = spec.warningEffectId() == null ? EffectIds.IMPACT : spec.warningEffectId().toString();
            emitImageEffect(center, spec.objectTexture().toString(), effectId, spec.effectScale(), Math.max(.25f, spec.delaySeconds() + .5f));
        }

        if (spec.warningEffectId() != null) {
            emitEffect(center, spec.warningEffectId().toString(), spec.effectScale(), Math.max(.05f, spec.delaySeconds()));
        }

        spawn(new ScheduledCombatAction(center, spec.delaySeconds(), () -> {
            float radiusSquared = spec.radius() * spec.radius();

            for (Object enemy : enemyBattleCombatants(source)) {
                if (positionOf(enemy).dst2(center) <= radiusSquared) {
                    applyCombatantDamage(source, enemy, spec.damage());
                }
            }

            if (spec.impactEffectId() != null) {
                emitEffect(center, spec.impactEffectId().toString(), spec.effectScale(), .5f);
            }
        }));
    }

    private static float damageTypeScale(DamageType type) {
        return switch (type) {
            case NORMAL -> 1f;
            case SKILL -> .55f;
            case ULTIMATE -> GLOBAL_ULTIMATE_DAMAGE_SCALE;
        };
    }

    private void requireOwnedFiniteAmount(CharacterRuntime target, float amount, String name) {
        if (!containsCombatant(target)) {
            throw new IllegalArgumentException("Combatant does not belong to this battle");
        }

        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    /**
     * 対象をsourceから離す候補を境界内で探し、他の生存メインと重ならない最遠点へ移動する。
     */
    public void knockbackAwayFrom(Object source, Object target, float distance) {
        if (!isActiveBattleCombatant(source) || !isResolvableBattleCombatant(target)) {
            throw new IllegalArgumentException("Knockback combatants must belong to this battle");
        }

        if (!Float.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("Knockback distance must be finite and non-negative");
        }

        if (distance == 0 || !alive(target)) {
            return;
        }

        Vector3 targetPosition = positionOf(target);
        Vector3 direction = new Vector3(targetPosition).sub(positionOf(source));
        direction.y = 0;

        if (direction.isZero() && target instanceof CharacterRuntime main) {
            direction.set(main.facing.x, 0, main.facing.z);
        }

        if (direction.isZero() || !Float.isFinite(direction.x) || !Float.isFinite(direction.z)) {
            direction.set(1, 0, 0);
        }

        direction.nor();

        float margin = Math.min(config.arenaHalfExtent, collisionRadiusOf(target));
        float limit = Math.max(0, config.arenaHalfExtent - margin);

        Vector3 candidate = new Vector3();

        for (int step = 20; step >= 0; step--) {
            float travel = distance * step / 20f;
            candidate.set(targetPosition).mulAdd(direction, travel);

            candidate.x = MathUtils.clamp(candidate.x, -limit, limit);
            candidate.z = MathUtils.clamp(candidate.z, -limit, limit);
            candidate.y = config.groundY;

            if (!(target instanceof CharacterRuntime main) || collisionFree(main, candidate)) {
                targetPosition.set(candidate);
                return;
            }
        }

        // 呼出し前の位置が不正でも、少なくとも有限な戦場内座標へ復旧する。
        targetPosition.set(MathUtils.clamp(targetPosition.x, -limit, limit), config.groundY, MathUtils.clamp(targetPosition.z, -limit, limit));
    }

    /**
     * 任意種別の生存戦闘個体を、別の個体へ向けて指定距離まで移動する。
     */
    public void pullToward(Object target, Object destination, float maximumDistance, float remainingDistance) {
        if (!isActiveBattleCombatant(target) || !isActiveBattleCombatant(destination)) {
            throw new IllegalArgumentException("Pull combatants must be active in this battle");
        }

        if (!Float.isFinite(maximumDistance) || maximumDistance < 0 || !Float.isFinite(remainingDistance) || remainingDistance < 0) {
            throw new IllegalArgumentException("Pull distances must be finite and non-negative");
        }

        Vector3 moving = positionOf(target);
        Vector3 direction = new Vector3(positionOf(destination)).sub(moving);
        direction.y = 0;

        float distance = direction.len();

        if (distance > 0) {
            moving.mulAdd(direction.scl(1f / distance), Math.min(maximumDistance, Math.max(0, distance - remainingDistance)));
            clampBattleCombatantToArena(target);
        }
    }

    /**
     * 状態管理を持つ個体の負の状態を解除し、メインでは行動不能も解除する。
     */
    public void cleanseNegativeStatuses(Object target) {
        if (!isActiveBattleCombatant(target)) {
            throw new IllegalArgumentException("Cleanse target must be active in this battle");
        }

        if (target instanceof CharacterRuntime main) {
            main.clearNegativeStatuses();

        } else {
            statusesOf(target).clearNegative();
        }
    }

    /**
     * 任意種別の戦闘個体を、当たり半径を考慮した戦場内座標へ収める。
     */
    private void clampBattleCombatantToArena(Object target) {
        Vector3 position = positionOf(target);
        float limit = Math.max(0f, config.arenaHalfExtent - collisionRadiusOf(target));

        position.x = MathUtils.clamp(position.x, -limit, limit);
        position.z = MathUtils.clamp(position.z, -limit, limit);
        position.y = config.groundY;
    }

    private static float collisionRadiusOf(Object target) {
        return switch (target) {
            case CharacterRuntime main -> Math.max(0f, main.definition.collisionRadius);
            case FamilySummon family -> Math.max(0f, family.collisionRadius());
            case null, default -> .5f;
        };
    }

    private boolean collisionFree(CharacterRuntime moving, Vector3 candidate) {
        for (CharacterRuntime other : allMains()) {

            if (other == moving || !other.isAlive()) {
                continue;
            }

            float minimum = moving.definition.collisionRadius + other.definition.collisionRadius;

            float dx = candidate.x - other.position.x;
            float dz = candidate.z - other.position.z;

            if (dx * dx + dz * dz < minimum * minimum) {
                return false;
            }
        }
        return true;
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

    /**
     * メイン、補助、召喚を含む、この戦闘が所有する全個体。
     */
    public List<Object> livingBattleCombatants() {
        List<Object> result = new ArrayList<>();

        for (CharacterRuntime main : allMains()) {
            if (main.isAlive()) {
                result.add(main);
            }
        }

        for (PlayerTeam team : teams.values()) {
            for (SupportCharacterRuntime support : team.supports()) {
                if (support.isAlive() && !support.hiddenAndDisabled()) {
                    result.add(support);
                }
            }
        }

        for (CombatObject object : objects) {
            if (isLivingSummon(object)) {
                result.add(object);
            }
        }

        for (CombatObject object : pendingObjects) {
            if (isLivingSummon(object) && !result.contains(object)) {
                result.add(object);
            }
        }

        return List.copyOf(result);
    }

    private static boolean isLivingSummon(Object value) {
        return value instanceof FamilySummon family && family.isAlive() || value instanceof TemporarySupportSummon temporary && !temporary.expired();
    }

    public boolean containsBattleCombatant(Object value) {
        return switch (value) {
            case CharacterRuntime main -> containsCombatant(main);
            case SupportCharacterRuntime support ->
                    teams.values().stream().flatMap(team -> team.supports().stream()).anyMatch(candidate -> candidate == support);

            case null, default -> (objects.contains(value) || pendingObjects.contains(value)) && isLivingSummon(value);
        };
    }

    /**
     * 公開Viewが指す個体が、この戦闘へ登録されたままかを返す。
     * 同一アクション中に死亡した個体は、更新終了時に除去されるまでは所属を維持する。
     */
    public boolean isResolvableBattleCombatant(Object value) {
        if (closed || result != null) {
            return false;
        }

        return switch (value) {
            case CharacterRuntime main -> containsCombatant(main);
            case SupportCharacterRuntime support ->
                    !support.hiddenAndDisabled() && teams.values().stream().flatMap(team -> team.supports().stream()).anyMatch(candidate -> candidate == support);

            case null, default ->
                    (value instanceof FamilySummon || value instanceof TemporarySupportSummon) && (objects.contains(value) || pendingObjects.contains(value));
        };
    }

    /**
     * 戦闘が継続中で、公開操作の対象として現在戦場に存在する個体かを返す。
     */
    public boolean isActiveBattleCombatant(Object value) {
        return isResolvableBattleCombatant(value) && alive(value);
    }

    public List<Object> alliedBattleCombatants(Object source) {
        PlayerId owner = ownerOf(source);
        return livingBattleCombatants().stream().filter(value -> ownerOf(value) == owner).toList();
    }

    public List<Object> enemyBattleCombatants(Object source) {
        PlayerId owner = ownerOf(source);
        return livingBattleCombatants().stream().filter(value -> ownerOf(value) != owner).toList();
    }

    public PlayerId ownerOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> main.playerId;
            case SupportCharacterRuntime support -> support.owner;
            case FamilySummon family -> family.owner;
            case TemporarySupportSummon temporary -> temporary.owner;

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public String definitionIdOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> main.definition.id;
            case SupportCharacterRuntime support -> support.definition.id;
            case FamilySummon familySummon -> "mojibattle:family_summon";
            case TemporarySupportSummon temporary -> temporary.definitionId;

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public String instanceIdOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> combatantInstanceIds.get(main);
            case SupportCharacterRuntime support -> support.instanceId;
            case TemporarySupportSummon temporary -> temporary.instanceId;
            case FamilySummon familySummon ->
                    summonInstanceIds.computeIfAbsent(value, ignored -> "summon:" + (++summonSequence));

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public CombatantKind kindOf(Object value) {
        switch (value) {
            case CharacterRuntime ignored -> {
                return CombatantKind.MAIN;
            }
            case SupportCharacterRuntime ignored -> {
                return CombatantKind.SUPPORT;
            }

            case null, default -> {
            }
        }

        if (value instanceof FamilySummon || value instanceof TemporarySupportSummon) {
            return CombatantKind.SUMMON;
        }

        throw new IllegalArgumentException("Unknown combatant");
    }

    public float hpOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> main.hp();
            case SupportCharacterRuntime support -> support.hp();
            case FamilySummon family -> family.hp();
            case TemporarySupportSummon temporary -> temporary.hp();

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public float maximumHpOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> main.maxHp();
            case SupportCharacterRuntime support -> support.maximumHp();
            case FamilySummon family -> family.maximumHp;
            case TemporarySupportSummon temporary -> temporary.maximumHp();

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public float ultimatePointsOf(Object value) {
        if (value instanceof CharacterRuntime main) {
            return main.ultimatePoints();
        }

        if (value instanceof SupportCharacterRuntime support) {
            return support.ultimatePoints();
        }

        return 0;
    }

    public boolean alive(Object value) {
        return switch (value) {
            case null -> false;
            case CharacterRuntime main -> main.isAlive();
            case SupportCharacterRuntime support -> support.isAlive();

            default -> isLivingSummon(value);
        };
    }

    public Vector3 positionOf(Object value) {
        return switch (value) {
            case CharacterRuntime main -> main.position;
            case SupportCharacterRuntime support -> support.position;
            case CombatObject object -> object.position();

            case null, default -> throw new IllegalArgumentException("Unknown combatant");
        };
    }

    public float combatantDefenseMultiplier(Object value) {
        float base = value instanceof CharacterRuntime main ? defenseMultiplier(main) : value instanceof SupportCharacterRuntime support ? support.defenseMultiplier() : value instanceof FamilySummon family ? family.defenseMultiplier() : 1f;
        DuelState duel = duelFor(instanceIdOf(value));

        if (duel == null) {
            return 1f;
        }

        return base * duel.participantDefenseMultiplier;
    }

    public Object findByInstanceId(String id) {
        if (id == null) {
            return null;
        }

        return livingBattleCombatants().stream().filter(value -> id.equals(instanceIdOf(value))).findFirst().orElse(null);
    }

    public void startDuel(DuelSpec spec, List<Object> allies, Object enemy) {
        if (spec == null || allies.isEmpty() || !containsBattleCombatant(enemy) || !alive(enemy)) {
            throw new IllegalArgumentException("Invalid duel");
        }

        PlayerId allyOwner = ownerOf(allies.getFirst());

        if (allies.stream().anyMatch(value -> !containsBattleCombatant(value) || !alive(value) || ownerOf(value) != allyOwner) || ownerOf(enemy) == allyOwner) {
            throw new IllegalArgumentException("Duel sides must be opposing teams");
        }

        Set<String> allyIds = new LinkedHashSet<>();
        allies.forEach(value -> allyIds.add(instanceIdOf(value)));

        Set<String> nextParticipants = new HashSet<>(allyIds);
        nextParticipants.add(instanceIdOf(enemy));

        List<DuelState> replaced = duels.stream().filter(value -> value.allies.stream().anyMatch(nextParticipants::contains) || nextParticipants.contains(value.enemy)).toList();

        for (DuelState old : replaced) {
            old.allies.forEach(normalAttackTargetLocks::remove);
            normalAttackTargetLocks.remove(old.enemy);
        }

        duels.removeAll(replaced);

        DuelState state = new DuelState(Set.copyOf(allyIds), instanceIdOf(enemy), timer.elapsed() + spec.durationSeconds(), spec.restrictTargets(), spec.participantDefenseMultiplier(), spec.outsideAttackerDamageMultiplier());

        duels.add(state);

        for (String id : allyIds) {
            normalAttackTargetLocks.put(id, new TargetLock(state.enemy, state.expiresAt));
        }

        normalAttackTargetLocks.put(state.enemy, new TargetLock(allyIds.iterator().next(), state.expiresAt));
    }

    public void lockNormalAttackTarget(Object attacker, Object target, float durationSeconds) {
        if (!containsBattleCombatant(attacker) || !containsBattleCombatant(target) || ownerOf(attacker) == ownerOf(target) || !Float.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Invalid normal attack target lock");
        }

        normalAttackTargetLocks.put(instanceIdOf(attacker), new TargetLock(instanceIdOf(target), timer.elapsed() + durationSeconds));
    }

    public Object lockedNormalAttackTarget(Object attacker) {
        TargetLock lock = normalAttackTargetLocks.get(instanceIdOf(attacker));
        return lock == null ? null : findByInstanceId(lock.targetInstanceId);
    }

    public boolean canDamage(Object attacker, Object target) {
        if (!containsBattleCombatant(attacker) || !containsBattleCombatant(target) || ownerOf(attacker) == ownerOf(target)) {
            return false;
        }

        DuelState duel = duelFor(instanceIdOf(attacker));

        if (duel == null || !duel.restrictTargets) {
            return true;
        }

        String attackerId = instanceIdOf(attacker), targetId = instanceIdOf(target);
        return duel.enemy.equals(attackerId) ? duel.allies.contains(targetId) : duel.enemy.equals(targetId);
    }

    public float duelDamageMultiplier(Object attacker, Object target) {
        String attackerId = instanceIdOf(attacker), targetId = instanceIdOf(target);
        DuelState targetDuel = duelFor(targetId);

        if (targetDuel == null) {
            return 1f;
        }

        boolean sameDuel = targetDuel.enemy.equals(attackerId) || targetDuel.allies.contains(attackerId);

        return sameDuel ? 1f : targetDuel.outsideAttackerDamageMultiplier;
    }

    private DuelState duelFor(String instanceId) {
        return duels.stream().filter(value -> value.enemy.equals(instanceId) || value.allies.contains(instanceId)).findFirst().orElse(null);
    }

    private record TargetLock(String targetInstanceId, float expiresAt) {
    }

    private record DuelState(Set<String> allies, String enemy, float expiresAt, boolean restrictTargets,
                             float participantDefenseMultiplier, float outsideAttackerDamageMultiplier) {
    }

    public CharacterRuntime targetOf(PlayerId owner) {
        CharacterRuntime source = player(owner), best = null;
        Object locked = lockedNormalAttackTarget(source);
        if (locked instanceof CharacterRuntime lockedMain && lockedMain.isAlive()) {
            return lockedMain;
        }

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

        float passiveMove = passiveProduct(value, passive -> passive.moveSpeedMultiplier(new CoreCombatantView(this, value)));
        float statusMove = value.statuses.multiplier(StatusType.MOVE_UP) * value.statuses.multiplier(StatusType.MOVE_DOWN);
        float effectiveMoveSpeed = value.definition.moveSpeed * 1.2f * passiveMove * fieldMoveMultiplier(value) * statusMove * (value.awakened() ? awakeningConfig.moveSpeedMultiplier : 1f);

        float cooldownMultiplier = config.skillCooldownMultiplier * passiveProduct(value, passive -> passive.skillCooldownMultiplier(view(value))) * value.statuses.multiplier(StatusType.COOLDOWN_REDUCTION) * fieldEffects.multiplier("cooldown", timer.elapsed(), value);
        float ultimateGainMultiplier = passiveProduct(value, passive -> passive.ultimateGainMultiplier(view(value))) * value.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP) * (value.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f);
        float criticalRate = MathUtils.clamp(criticalConfig.baseRate + value.definition.criticalRate + value.statuses.additive(StatusType.CRITICAL_RATE_UP) + fieldEffects.additive("critical_rate", value) + fieldEffects.additive("critical_enable", value) + passiveCriticalRateModifier(value), 0, 1);
        float criticalDamage = Math.max(criticalConfig.baseDamageMultiplier, value.definition.criticalDamageMultiplier + fieldEffects.additive("critical_damage", value)) * value.statuses.multiplier(StatusType.CRITICAL_DAMAGE_UP);

        return new CharacterDebugSnapshot(value.definition.id, value.hp(), value.maxHp(), value.missingHp(), value.definition.baseAttack, attackMultiplier(value), value.definition.moveSpeed, effectiveMoveSpeed, incomingDamageMultiplier(value), value.definition.collisionRadius, passiveHitboxMultiplier(id), value.skillCooldown(), value.skillChargeRatio(), value.skillStocks(), value.maximumSkillStocks(), value.ultimatePoints(), value.totalDamage(), value.stunRemaining(), value.movementLocked(), value.awakened(), value.isAlive(), value.zeroDefense(), value.position.x, value.position.y, value.position.z, value.facing.x, value.facing.z, cooldownMultiplier, ultimateGainMultiplier, passiveProduct(value, passive -> passive.stunMultiplier(view(value))), ultimateDamageMultiplier(value), criticalRate, criticalDamage, value.definition.criticalEnabled || fieldEffects.hasType("critical_enable") || value.statuses.has(StatusType.CRITICAL_RATE_UP) || passiveCriticalRateModifier(value) != 0f);
    }

    public float fieldProjectileCurve() {
        return fieldEffects.additive("projectile_curve");
    }

    public float fieldMoveMultiplier() {
        return fieldEffects.multiplier("move_multiplier", timer.elapsed()) * fieldEffects.timedMultiplier("speed_pulse", timer.elapsed()) * fieldEffects.multiplier("express_maki", timer.elapsed());
    }

    public float fieldMoveMultiplier(CharacterRuntime target) {
        return fieldEffects.multiplier("move_multiplier", timer.elapsed(), target) * fieldEffects.multiplier("express_maki", timer.elapsed(), target) * fieldEffects.timedMultiplier("speed_pulse", timer.elapsed());
    }

    public float fieldMoveMultiplier(SupportCharacterRuntime target) {
        return fieldEffects.multiplier("move_multiplier", timer.elapsed(), target) * fieldEffects.multiplier("express_maki", timer.elapsed(), target) * fieldEffects.timedMultiplier("speed_pulse", timer.elapsed());
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
        return baseSeconds * config.skillCooldownMultiplier * passiveProduct(owner, value -> value.skillCooldownMultiplier(view(owner))) * owner.statuses.multiplier(StatusType.COOLDOWN_REDUCTION) * fieldEffects.multiplier("cooldown", timer.elapsed(), owner);
    }

    public void addSkillUltimatePoints(CharacterRuntime owner, float basePoints) {
        owner.addUltimatePoints(basePoints * passiveProduct(owner, value -> value.ultimateGainMultiplier(view(owner))) * owner.statuses.multiplier(StatusType.ULTIMATE_GAIN_UP) * (owner.awakened() ? awakeningConfig.ultimateGainMultiplier : 1f));
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

    private List<PassiveRuntime> passives(CharacterRuntime owner) {
        if (owner.definition.trainingDummy) {
            return List.of();
        }

        AbilityLoadout loadout = abilityLoadouts.get(owner);
        return loadout == null ? List.of() : loadout.passives();
    }

    private void notifySkillActivated(CharacterRuntime owner) {
        for (PassiveRuntime passive : passives(owner)) {
            passive.onSkillActivated(ExternalAbilityContexts.create(this, owner));
        }
    }

    private void notifyUltimateActivated(CharacterRuntime owner) {
        for (PassiveRuntime passive : passives(owner)) {
            passive.onUltimateActivated(ExternalAbilityContexts.create(this, owner));
        }
    }

    private void notifyDamagePassives(DamageEvent event) {
        if (event.amount() <= 0) {
            return;
        }
        DamageEventContext context = damageContext(event);

        for (PassiveRuntime passive : passives(event.attacker())) {
            passive.onDamageDealt(context);
        }

        for (PassiveRuntime passive : passives(event.target())) {
            passive.onDamageReceived(context);
        }

        event.target().statuses.consumeDamageReceived();
    }

    private DamageEventContext damageContext(DamageEvent event) {
        return new DamageEventContext(new CoreCombatantView(this, event.attacker()), new CoreCombatantView(this, event.target()), event.amount(), new CoreCombatActions(this), elapsedSeconds());
    }

    private float passiveProduct(CharacterRuntime owner, java.util.function.Function<PassiveRuntime, Float> value) {
        float result = 1f;

        for (PassiveRuntime passive : passives(owner)) {
            result *= value.apply(passive);
        }

        return result;
    }

    private CoreCombatantView view(CharacterRuntime owner) {
        return new CoreCombatantView(this, owner);
    }

    private boolean passiveIgnoresDefense(CharacterRuntime owner) {
        return passives(owner).stream().anyMatch(passive -> passive.ignoresDefense(view(owner)));
    }

    private record NormalAttackProfile(float intervalMultiplier, int projectileCount, float spreadDegrees,
                                       int hitsPerAttack, float damageMultiplier, String texture, String effectId) {
    }

    /**
     * 全パッシブの通常攻撃補正を毎攻撃時に決定的な順序で合成する。
     */
    private NormalAttackProfile normalAttackProfile(CharacterRuntime owner) {
        List<NormalAttackModifier> modifiers = passives(owner).stream().flatMap(passive -> passive.normalAttackModifiers(view(owner)).stream()).sorted(Comparator.comparingInt(NormalAttackModifier::priority).thenComparing(value -> value.id().toString())).toList();

        float interval = fieldEffects.multiplier("normal_attack_interval", timer.elapsed(), owner) * owner.statuses.multiplier(StatusType.NORMAL_ATTACK_INTERVAL);

        int projectileCount = 1;
        float spread = 0f;
        int hits = 1;
        float damage = 1f;

        String texture = owner.normalAttackTexture();
        String effect = EffectIds.IMPACT;

        for (NormalAttackModifier modifier : modifiers) {

            interval *= modifier.attackIntervalMultiplier();
            projectileCount += modifier.projectileCountDelta();
            spread += modifier.spreadDegrees();
            hits += modifier.hitsPerAttackDelta();
            damage *= modifier.damageMultiplier();

            if (modifier.projectileTexture() != null) {
                texture = modifier.projectileTexture().toString();
            }
            if (modifier.effectId() != null) {
                effect = modifier.effectId().toString();
            }
        }
        return new NormalAttackProfile(MathUtils.clamp(interval, .05f, 10f), MathUtils.clamp(projectileCount, 1, 32), MathUtils.clamp(spread, 0f, 360f), MathUtils.clamp(hits, 1, 16), MathUtils.clamp(damage, 0f, 100f), texture, effect);
    }

    /**
     * HUD向け状態行をpriority降順で最大8件返す。
     */
    public List<AbilityStateLine> abilityStateLines(CharacterRuntime owner) {
        if (!containsCombatant(owner)) {
            throw new IllegalArgumentException("Combatant does not belong to this battle");
        }

        return passives(owner).stream().flatMap(passive -> passive.stateLines(view(owner)).stream()).sorted(Comparator.comparingInt(AbilityStateLine::priority).reversed().thenComparing(AbilityStateLine::translationKey)).limit(8).toList();
    }

    private float passiveCriticalRateModifier(CharacterRuntime owner) {
        float result = 0f;
        for (PassiveRuntime passive : passives(owner)) {
            float modifier = passive.criticalRateModifier(view(owner));

            if (!Float.isFinite(modifier)) {
                throw new IllegalArgumentException("Passive critical rate modifier must be finite");
            }
            result += modifier;
        }
        return result;
    }

    /**
     * 現在の固定ステップ基準戦闘経過秒。
     */
    public float elapsedSeconds() {
        return timer.elapsed();
    }

    private float ultimateDamageMultiplier(CharacterRuntime owner) {
        return passiveProduct(owner, value -> value.ultimateDamageMultiplier(view(owner)));
    }

    /**
     * 必殺技の反復攻撃へ加算する、装備中パッシブ由来の追加回数を返す。
     */
    public long additionalUltimateAttackCount(CharacterRuntime owner) {
        long result = 0L;
        for (PassiveRuntime passive : passives(owner)) {
            long addition = Math.max(0L, passive.additionalUltimateAttackCount(view(owner)));

            if (Long.MAX_VALUE - result < addition) {
                return Long.MAX_VALUE;
            }

            result += addition;
        }
        return result;
    }
}
