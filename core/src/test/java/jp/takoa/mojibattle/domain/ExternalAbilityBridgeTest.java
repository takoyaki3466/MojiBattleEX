package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector2;
import jp.takoa.mojibattle.ability.AbilityActivationContext;
import jp.takoa.mojibattle.ability.PassiveRuntime;
import jp.takoa.mojibattle.battle.DamageType;
import jp.takoa.mojibattle.battle.DamageRequest;
import jp.takoa.mojibattle.battle.DamageResult;
import jp.takoa.mojibattle.battle.DelayedStrikeSpec;
import jp.takoa.mojibattle.battle.StatusDuration;
import jp.takoa.mojibattle.battle.StatusEffectType;
import jp.takoa.mojibattle.battle.StatusSpec;
import jp.takoa.mojibattle.battle.StatusStacking;
import jp.takoa.mojibattle.battle.StatusConsumption;
import jp.takoa.mojibattle.battle.NormalAttackModifier;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.config.AwakeningConfig;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.config.CriticalConfig;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalPassiveAdapter;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalSkillAdapter;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalUltimateAdapter;
import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.event.gameplay.DamageEvent;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ExternalAbilityBridgeTest {
    @Test
    void successfulActivationNotifiesOnlyOwnersPassiveAndUltimateUsesDynamicMultiplier() {
        List<RankPassive> instances = new ArrayList<>();
        AbilityConfigs.ProjectileSkill skillConfig = new AbilityConfigs.ProjectileSkill();
        skillConfig.id = "test:skill";
        skillConfig.cooldownSeconds = 2f;
        AbilityConfigs.AreaUltimate ultimateConfig = new AbilityConfigs.AreaUltimate();
        ultimateConfig.id = "test:ultimate";
        ultimateConfig.nameKey = "test.ultimate";
        ultimateConfig.pointCost = 100f;

        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig, context -> true));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig, context -> {
            context.actions().enemiesOf(context.user()).forEach(enemy ->
                    context.actions().damage(context.user(), enemy, 100f, DamageType.ULTIMATE));
            return true;
        }));
        abilities.registerPassive("test:passive", () -> {
            RankPassive passive = new RankPassive();
            instances.add(passive);
            return new ExternalPassiveAdapter("test:passive", passive);
        });
        abilities.freeze();

        EventBus events = new EventBus();
        List<DamageEvent> damageEvents = new ArrayList<>();
        events.subscribe(DamageEvent.class, damageEvents::add);
        BattleController battle = battle(abilities, events, List.of(team("same"), team("same")));

        EnumMap<PlayerId, PlayerCommand> commands = new EnumMap<>(PlayerId.class);
        commands.put(PlayerId.ONE, command(true, false));
        battle.update(.016f, commands);
        assertEquals(1, instances.get(0).skillActivations);
        assertEquals(0, instances.get(1).skillActivations);

        battle.update(.016f, commands);
        assertEquals(1, instances.get(0).skillActivations, "cooldown failure must not notify");

        battle.player(PlayerId.ONE).maximizeUltimate();
        commands.put(PlayerId.ONE, command(false, true));
        battle.update(.016f, commands);

        assertEquals(1, instances.get(0).ultimateActivations);
        assertEquals(0, instances.get(1).ultimateActivations);
        assertTrue(damageEvents.stream().anyMatch(event -> Math.abs(event.amount() - 130f) < .001f),
                "100 base * .65 ultimate scale * 2 dynamic passive multiplier");
    }

    @Test
    void enemyEnumerationExcludesTeamAndDeadAndKnockbackStaysFiniteInsideArena() {
        AbilityRegistry abilities = noOpAbilities();
        BattleTeamDefinition first = new BattleTeamDefinition(character("one"), List.of(character("ally")), List.of());
        BattleTeamDefinition second = new BattleTeamDefinition(character("two"), List.of(character("enemy_sub")), List.of());
        BattleController battle = battle(abilities, new EventBus(), List.of(first, second));
        CharacterRuntime source = battle.player(PlayerId.ONE);

        List<CharacterRuntime> enemies = battle.enemiesOf(source);
        assertEquals(2, enemies.size());
        assertTrue(enemies.stream().allMatch(enemy -> enemy.playerId == PlayerId.TWO));

        CharacterRuntime target = battle.player(PlayerId.TWO);
        source.position.set(3f, 0, 3f);
        target.position.set(4f, 0, 4f);
        battle.knockbackAwayFrom(source, target, 100f);
        assertTrue(Float.isFinite(target.position.x) && Float.isFinite(target.position.z));
        assertTrue(Math.abs(target.position.x) <= 4.5f && Math.abs(target.position.z) <= 4.5f);

        enemies.get(1).receiveDamage(10_000f);
        assertEquals(1, battle.enemiesOf(source).size());
        assertEquals(2, battle.alliesOf(source).size(), "allies include the source and living sub-mains");
    }

    @Test
    void passiveUpdateIsPerOwnerAndDynamicCriticalCanIgnoreDefense() {
        List<UpdatingPassive> instances = new ArrayList<>();
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> false));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> {
            UpdatingPassive passive = new UpdatingPassive();
            instances.add(passive);
            return new ExternalPassiveAdapter("test:passive", passive);
        });
        abilities.freeze();
        BattleController battle = battle(abilities, new EventBus(), List.of(team("one"), team("two")));

        battle.update(.25f, new EnumMap<>(PlayerId.class));
        assertEquals(2, instances.size());
        assertEquals(1, instances.get(0).updates);
        assertEquals(1, instances.get(1).updates);
        assertNotSame(instances.get(0), instances.get(1));
        assertEquals(.25f, instances.get(0).elapsed, .0001f);

        DamageResult result = battle.applyAbilityDamage(battle.player(PlayerId.ONE),
                battle.player(PlayerId.TWO), new DamageRequest(100f, DamageType.NORMAL, false, true));
        assertTrue(result.critical());
        assertTrue(result.defenseIgnored());
        assertEquals(150f, result.dealtDamage(), .001f);
    }

    @Test
    void defenseIgnoreStillAppliesIncomingStatusMultiplier() {
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> {
            context.actions().applyStatus(context.user(), context.target(), new StatusSpec(
                    ResourceId.parse("test:vulnerability"), StatusEffectType.INCOMING_DAMAGE_MULTIPLIER,
                    1.25f, StatusDuration.permanent(), StatusStacking.REPLACE, 1, 1f));
            return true;
        }));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> new ExternalPassiveAdapter("test:passive", new PassiveRuntime() { }));
        abilities.freeze();
        BattleController battle = battle(abilities, new EventBus(), List.of(team("one"), team("two")));
        EnumMap<PlayerId, PlayerCommand> commands = new EnumMap<>(PlayerId.class);
        commands.put(PlayerId.ONE, command(true, false));
        battle.update(.1f, commands);

        DamageResult result = battle.applyAbilityDamage(battle.player(PlayerId.ONE), battle.player(PlayerId.TWO),
                new DamageRequest(100f, DamageType.NORMAL, true, false));
        assertTrue(result.defenseIgnored());
        assertEquals(125f, result.dealtDamage(), .001f,
                "defense ignore must not bypass status or field vulnerability");
    }

    @Test
    void delayedStrikeRunsOnceAtFixedStepBoundary() {
        EventBus events = new EventBus();
        List<DamageEvent> damageEvents = new ArrayList<>();
        events.subscribe(DamageEvent.class, damageEvents::add);
        BattleController battle = battle(noOpAbilities(), events,
                List.of(team("one"), team("two")));
        CharacterRuntime source = battle.player(PlayerId.ONE);
        CharacterRuntime target = battle.player(PlayerId.TWO);
        float before = target.hp();
        battle.spawnDelayedStrike(source, target, new DelayedStrikeSpec(.5f,
                DamageRequest.of(100f, DamageType.NORMAL), ResourceId.parse("test:impact"), null, 1f));

        battle.update(.25f, new EnumMap<>(PlayerId.class));
        assertEquals(before, target.hp());
        battle.update(.25f, new EnumMap<>(PlayerId.class));
        assertEquals(before, target.hp(), "pending objects start updating on the next fixed step");
        battle.update(.25f, new EnumMap<>(PlayerId.class));
        assertEquals(before - 100f, target.hp(), .001f);
        battle.update(.25f, new EnumMap<>(PlayerId.class));
        assertEquals(1, damageEvents.stream().filter(event -> event.amount() == 100f).count(),
                "the scheduled action fires exactly once");
    }

    @Test
    void publicStatusIsReevaluatedAndRestoresBaseValueAfterExpiry() {
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> {
            context.actions().applyStatus(context.user(), context.user(), new StatusSpec(
                    ResourceId.parse("test:temporary_power"), StatusEffectType.ATTACK_MULTIPLIER,
                    2f, StatusDuration.seconds(.5f), StatusStacking.REFRESH, 1, 1f));
            return true;
        }));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> new ExternalPassiveAdapter("test:passive", new PassiveRuntime() { }));
        abilities.freeze();
        BattleController battle = battle(abilities, new EventBus(), List.of(team("one"), team("two")));
        EnumMap<PlayerId, PlayerCommand> commands = new EnumMap<>(PlayerId.class);
        commands.put(PlayerId.ONE, command(true, false));
        battle.update(.1f, commands);

        DamageResult boosted = battle.applyAbilityDamage(battle.player(PlayerId.ONE),
                battle.player(PlayerId.TWO), DamageRequest.of(100f, DamageType.NORMAL));
        assertEquals(200f, boosted.dealtDamage(), .001f);

        commands.put(PlayerId.ONE, command(false, false));
        battle.update(.5f, commands);
        DamageResult restored = battle.applyAbilityDamage(battle.player(PlayerId.ONE),
                battle.player(PlayerId.TWO), DamageRequest.of(100f, DamageType.NORMAL));
        assertEquals(100f, restored.dealtDamage(), .001f);
    }

    @Test
    void nextIncomingDamageStatusIsConsumedOnce() {
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> {
            context.actions().applyStatus(context.user(), context.target(), new StatusSpec(
                    ResourceId.parse("test:shield"), StatusEffectType.NEXT_INCOMING_DAMAGE_MULTIPLIER,
                    .5f, StatusDuration.permanent(), StatusStacking.REPLACE, 1, 1f,
                    StatusConsumption.ON_DAMAGE_RECEIVED));
            return true;
        }));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> new ExternalPassiveAdapter("test:passive", new PassiveRuntime() { }));
        abilities.freeze();
        BattleController battle = battle(abilities, new EventBus(), List.of(team("one"), team("two")));
        EnumMap<PlayerId, PlayerCommand> commands = new EnumMap<>(PlayerId.class);
        commands.put(PlayerId.ONE, command(true, false));
        battle.update(.1f, commands);

        DamageResult first = battle.applyAbilityDamage(battle.player(PlayerId.ONE), battle.player(PlayerId.TWO),
                DamageRequest.of(100f, DamageType.NORMAL));
        DamageResult second = battle.applyAbilityDamage(battle.player(PlayerId.ONE), battle.player(PlayerId.TWO),
                DamageRequest.of(100f, DamageType.NORMAL));
        assertEquals(50f, first.dealtDamage(), .001f);
        assertEquals(100f, second.dealtDamage(), .001f);
    }

    @Test
    void normalAttackModifiersComposeAndHudLinesAreOrdered() {
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> false));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> new ExternalPassiveAdapter("test:passive", new PassiveRuntime() {
            @Override
            public List<NormalAttackModifier> normalAttackModifiers(jp.takoa.mojibattle.battle.CombatantView owner) {
                return List.of(new NormalAttackModifier(ResourceId.parse("test:multi"), 1, 1f,
                        2, 20f, 1, 1f, null, null));
            }

            @Override
            public List<AbilityStateLine> stateLines(jp.takoa.mojibattle.battle.CombatantView owner) {
                return List.of(AbilityStateLine.of("test.low", 1), AbilityStateLine.of("test.high", 10));
            }
        }));
        abilities.freeze();
        BattleController battle = battle(abilities, new EventBus(), List.of(team("one"), team("two")));
        battle.update(.01f, new EnumMap<>(PlayerId.class));

        long shots = battle.objects().stream().filter(Projectile.class::isInstance)
                .map(Projectile.class::cast).filter(value -> value.owner == PlayerId.ONE).count();
        assertEquals(6, shots, "(1 + 2 projectiles) * (1 + 1 hits)");
        assertEquals(List.of("test.high", "test.low"), battle.abilityStateLines(battle.player(PlayerId.ONE))
                .stream().map(AbilityStateLine::translationKey).toList());
    }

    private static AbilityRegistry noOpAbilities() {
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.registerSkill("test:skill", () -> new ExternalSkillAdapter(skillConfig(), context -> false));
        abilities.registerUltimate("test:ultimate", () -> new ExternalUltimateAdapter(ultimateConfig(), context -> false));
        abilities.registerPassive("test:passive", () -> new ExternalPassiveAdapter("test:passive", new PassiveRuntime() { }));
        abilities.freeze();
        return abilities;
    }

    private static BattleController battle(AbilityRegistry abilities, EventBus events, List<BattleTeamDefinition> teams) {
        BattleConfig config = new BattleConfig();
        config.durationSeconds = 300f;
        config.fixedStepSeconds = .016f;
        config.finalMinuteSeconds = 60f;
        config.finalMinuteAttackMultiplier = 2f;
        config.arenaHalfExtent = 5f;
        config.groundY = 0f;
        config.skillCooldownMultiplier = 1f;
        config.healthMultiplier = 1f;
        config.chaosHealthMultiplier = 1f;
        CriticalConfig critical = new CriticalConfig();
        critical.baseRate = 0f;
        critical.baseDamageMultiplier = 1.5f;
        AwakeningConfig awakening = new AwakeningConfig();
        awakening.spawnAtRemainingSeconds = 30f;
        awakening.attackMultiplier = 1f;
        awakening.defenseMultiplier = 1f;
        awakening.moveSpeedMultiplier = 1f;
        awakening.ultimateGainMultiplier = 1f;
        awakening.pickupRadius = 1f;
        awakening.preferredDistance = 2f;
        Registry<SupportAbilityImplementation> supports = new Registry<>("test support", SupportAbilityImplementation::id);
        return new BattleController(config, critical, awakening, abilities, BattleMode.VERSUS,
                teams, List.of(), supports, events);
    }

    private static BattleTeamDefinition team(String id) {
        return new BattleTeamDefinition(character(id), List.of(), List.of());
    }

    private static CharacterDefinition character(String id) {
        CharacterDefinition definition = new CharacterDefinition();
        definition.id = id;
        definition.displayGlyph = id;
        definition.maxHp = 1000f;
        definition.baseAttack = 1f;
        definition.moveSpeed = 1f;
        definition.collisionRadius = .5f;
        definition.skillId = "test:skill";
        definition.ultimateId = "test:ultimate";
        definition.passiveId = "test:passive";
        definition.criticalDamageMultiplier = 1.5f;
        return definition;
    }

    private static AbilityConfigs.ProjectileSkill skillConfig() {
        AbilityConfigs.ProjectileSkill config = new AbilityConfigs.ProjectileSkill();
        config.id = "test:skill";
        config.cooldownSeconds = 1f;
        return config;
    }

    private static AbilityConfigs.AreaUltimate ultimateConfig() {
        AbilityConfigs.AreaUltimate config = new AbilityConfigs.AreaUltimate();
        config.id = "test:ultimate";
        config.nameKey = "test.ultimate";
        config.pointCost = 100f;
        return config;
    }

    private static PlayerCommand command(boolean skill, boolean ultimate) {
        return new PlayerCommand(new Vector2(), skill, ultimate, false, false);
    }

    private static final class RankPassive implements PassiveRuntime {
        int skillActivations;
        int ultimateActivations;

        @Override
        public void onSkillActivated(AbilityActivationContext context) {
            skillActivations++;
        }

        @Override
        public void onUltimateActivated(AbilityActivationContext context) {
            ultimateActivations++;
        }

        @Override
        public float ultimateDamageMultiplier(jp.takoa.mojibattle.battle.CombatantView owner) {
            return skillActivations > 0 ? 2f : 1f;
        }
    }

    private static final class UpdatingPassive implements PassiveRuntime {
        int updates;
        float elapsed;

        @Override
        public void update(jp.takoa.mojibattle.ability.PassiveUpdateContext context) {
            updates++;
            elapsed = context.battleElapsedSeconds();
        }

        @Override
        public float criticalRateModifier(jp.takoa.mojibattle.battle.CombatantView owner) {
            return 1f;
        }
    }
}
