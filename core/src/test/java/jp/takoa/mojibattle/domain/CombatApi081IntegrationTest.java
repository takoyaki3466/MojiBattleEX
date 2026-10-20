package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.ability.support.SupportSplitMemberSpec;
import jp.takoa.mojibattle.ability.support.SupportSplitSpec;
import jp.takoa.mojibattle.battle.action.*;
import jp.takoa.mojibattle.battle.action.spec.*;
import jp.takoa.mojibattle.battle.combatant.*;
import jp.takoa.mojibattle.battle.damage.*;
import jp.takoa.mojibattle.battle.presentation.*;
import jp.takoa.mojibattle.battle.rule.*;
import jp.takoa.mojibattle.battle.status.*;
import jp.takoa.mojibattle.config.*;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatActions;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatantView;
import jp.takoa.mojibattle.domain.ability.adapter.CoreUnifiedCombatantView;
import jp.takoa.mojibattle.domain.combat.TemporarySupportSummon;
import jp.takoa.mojibattle.domain.support.*;
import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.registry.*;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.registry.impl.SupportPassiveImpl;
import jp.takoa.mojibattle.resource.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 0.8.1の統一戦闘個体、決闘、分裂を実際のBattleControllerへ接続して検証する。 */
final class CombatApi081IntegrationTest {
    @Test void highestMaximumHpEnemyCanBeSelectedAsDuelTarget() {
        Fixture f = fixture();
        CombatantView chosen = f.actions.battlefield().enemiesOf(f.one).stream()
                .max(java.util.Comparator.comparing(CombatantView::maximumHp)).orElseThrow();
        assertEquals("test:enemy-high", chosen.id());
        f.actions.startDuel(new DuelSpec(List.of(f.one), chosen, 5, true, .5f, .2f));
        assertSame(f.battle.findByInstanceId(chosen.instanceId()), f.battle.lockedNormalAttackTarget(f.battle.player(PlayerId.ONE)));
    }

    @Test void duelRestrictsTargetsAndAppliesDefenseAndOutsideDamageByPair() {
        Fixture f = fixture();
        f.actions.startDuel(new DuelSpec(List.of(f.one), f.two, 5, true, .5f, .2f));
        assertEquals(.5f, f.actions.battlefield().livingCombatants().stream()
                .filter(value -> value.instanceId().equals(f.one.instanceId())).findFirst().orElseThrow().defenseMultiplier(), .0001f);
        assertEquals(0, f.actions.damage(f.one, f.three, DamageRequest.of(100, DamageType.NORMAL)).dealtDamage());
        float insider = f.actions.damage(f.two, f.one, DamageRequest.of(100, DamageType.NORMAL)).dealtDamage();
        float outsider = f.actions.damage(f.three, f.one, DamageRequest.of(100, DamageType.NORMAL)).dealtDamage();
        assertEquals(insider * .2f, outsider, .0001f);
    }

    @Test void splitSummonsShareDuelTargetThenDisappearAndRecombine() {
        Fixture f = fixture();
        var support = f.battle.support(PlayerId.ONE);
        var members = java.util.stream.IntStream.range(0, 5).mapToObj(index ->
                new SupportSplitMemberSpec("㌠", ResourceId.parse("mojibattle:glyphs/test.png"), 10, .5f, 8)).toList();
        new CoreSupportActions(f.battle, support).splitTemporarily(new SupportSplitSpec(members, .5f, f.two, true, true));
        List<CombatantView> split = f.actions.battlefield().alliesOf(f.one).stream()
                .filter(value -> value.kind() == CombatantKind.SUMMON).toList();
        assertEquals(5, split.size());
        f.actions.startDuel(new DuelSpec(split, f.two, .4f, true, .5f, .2f));
        for (CombatantView member : split) {
            Object runtime = f.battle.findByInstanceId(member.instanceId());
            assertInstanceOf(TemporarySupportSummon.class, runtime);
            assertSame(f.battle.player(PlayerId.TWO), f.battle.lockedNormalAttackTarget(runtime));
        }
        f.battle.update(.01f, commands());
        f.battle.update(.6f, commands());
        assertEquals(0, f.actions.battlefield().livingCombatants().stream()
                .filter(value -> value.kind() == CombatantKind.SUMMON && value.instanceId().startsWith(support.instanceId)).count());
        assertTrue(f.actions.battlefield().livingCombatants().stream().anyMatch(value -> value.instanceId().equals(support.instanceId)));
    }

    @Test void countMultiplierIncludesMainsSupportsAndSummons() {
        Fixture f = fixture();
        var support = f.battle.support(PlayerId.ONE);
        var members = java.util.stream.IntStream.range(0, 5).mapToObj(index ->
                new SupportSplitMemberSpec("㌠", ResourceId.parse("mojibattle:glyphs/test.png"), 10, 1, 8)).toList();
        new CoreSupportActions(f.battle, support).splitTemporarily(new SupportSplitSpec(members, 2, f.two, true, true));
        new CoreSupportActions(f.battle, f.battle.support(PlayerId.TWO))
                .setBattlefieldCountAttackMultiplier(ResourceId.parse("test:headcount"), 10, 0, .1f);
        assertEquals(10, f.actions.battlefield().livingCombatants().size());
        assertEquals(1f, f.battle.support(PlayerId.TWO).countAttackMultiplier(10), .0001f);
    }

    @Test void sameDefinitionOnOpposingSidesKeepsInstanceStateSeparate() {
        Fixture f = fixtureSameDefinitions();
        assertEquals(f.one.id(), f.two.id());
        assertNotEquals(f.one.instanceId(), f.two.instanceId());
        f.actions.damage(f.one, f.two, DamageRequest.of(20, DamageType.NORMAL));
        assertEquals(f.one.maximumHp(), f.one.hp(), .0001f);
        assertTrue(f.two.hp() < f.two.maximumHp());
    }

    @Test void supportIsAPositionedDamageAndStatusTarget() {
        Fixture f = fixture();
        CombatantView support = f.actions.battlefield().enemiesOf(f.one).stream()
                .filter(value -> value.kind() == CombatantKind.SUPPORT).findFirst().orElseThrow();
        assertTrue(support.alive());
        assertNotEquals(CombatantPosition.ORIGIN, support.position());
        f.actions.applyStatus(f.one, support, StatusSpec.multiplier(ResourceId.parse("test:exposed"),
                StatusEffectType.INCOMING_DAMAGE_MULTIPLIER, 2f, StatusDuration.seconds(3)));
        float dealt = f.actions.damage(f.one, support, DamageRequest.of(10, DamageType.NORMAL)).dealtDamage();
        assertEquals(20f, dealt, .0001f);
        assertEquals(80f, support.hp(), .0001f);
    }

    @Test void externalCombatActionsAcceptUnifiedMainSupportAndSummonTargets() {
        Fixture f = fixture();
        CombatantView source = new CoreCombatantView(f.battle, f.battle.player(PlayerId.ONE));
        CombatantView enemySupport = f.actions.battlefield().enemiesOf(source).stream()
                .filter(value -> value.kind() == CombatantKind.SUPPORT).findFirst().orElseThrow();

        assertDoesNotThrow(() -> f.actions.spawnProjectile(source, enemySupport, new ProjectileSpec(
                DamageRequest.of(10, DamageType.SKILL), 12, .5f, 2, 2, 20, 1,
                ResourceId.parse("test:glyphs/projectile.png"), ResourceId.parse("test:impact"))));
        assertDoesNotThrow(() -> f.actions.knockbackAwayFrom(source, enemySupport, 2));
        assertDoesNotThrow(() -> f.actions.mirrorAcrossArenaCenter(enemySupport));
        assertDoesNotThrow(() -> f.actions.swapPositions(source, enemySupport));

        DomainSpec domain = new DomainSpec(ResourceId.parse("test:support-domain"), 4,
                DomainVisualStyle.BLOOM, .8f, ResourceId.parse("test:star"));
        assertDoesNotThrow(() -> f.actions.activateDomain(enemySupport, domain));
        assertTrue(f.actions.isDomainActive(enemySupport, domain.id()));
        assertDoesNotThrow(() -> f.actions.spawnAreaAttack(enemySupport, source, new AreaAttackSpec(
                0, 5, DamageRequest.of(10, DamageType.ULTIMATE),
                ResourceId.parse("test:warning"), ResourceId.parse("test:impact"), null, 2)));
    }

    @Test void cinematicAndBattlefieldPulseActionsRemainValidForMainCharacters() {
        Fixture f = fixture();
        CombatantView source = new CoreCombatantView(f.battle, f.battle.player(PlayerId.ONE));
        assertDoesNotThrow(() -> f.actions.showCutIn(source, new CutInSpec(
                ResourceId.parse("test:cutins/slash.png"), "test.cutin", 2, 100,
                CutInMotion.SLASH_RUSH, 1.2f)));
        assertDoesNotThrow(() -> f.actions.startBattlefieldPulse(source, new BattlefieldPulseSpec(
                3, .5f, 4, DamageRequest.of(5, DamageType.SKILL),
                ResourceId.parse("test:slash"), 5)));
        assertNotNull(f.battle.cutIn());
    }

    @Test void simultaneousCutInsAreRetainedAndSettingCanDisableThem() {
        Fixture f = fixture();
        CombatantView first = new CoreCombatantView(f.battle, f.battle.player(PlayerId.ONE));
        CombatantView second = new CoreCombatantView(f.battle, f.battle.player(PlayerId.TWO));
        f.actions.showCutIn(first, new CutInSpec(ResourceId.parse("test:first.png"), "test.first", 2, 10));
        f.actions.showCutIn(second, new CutInSpec(ResourceId.parse("test:second.png"), "test.second", 2, 10));
        assertEquals(2, f.battle.cutIns().size());

        f.battle.setCutInsEnabled(false);
        assertTrue(f.battle.cutIns().isEmpty());
        f.actions.showCutIn(first, new CutInSpec(ResourceId.parse("test:hidden.png"), "test.hidden", 2, 10));
        assertTrue(f.battle.cutIns().isEmpty());
    }

    @Test void defaultUltimateCutInUsesCharacterImage() {
        Fixture f = fixture();
        CharacterRuntime source = f.battle.player(PlayerId.ONE);
        f.battle.showDefaultUltimateCutIn(source, source.definition.glyphTexture, "test.ultimate");
        assertEquals(ResourceId.parse("test:glyphs/test.png"), f.battle.cutIns().getFirst().spec().image());
    }

    private static EnumMap<PlayerId, PlayerCommand> commands() { return new EnumMap<>(PlayerId.class); }

    private static Fixture fixture() { return fixture(false); }
    private static Fixture fixtureSameDefinitions() { return fixture(true); }
    private static Fixture fixture(boolean same) {
        AbilityRegistry abilities = new AbilityRegistry(); abilities.freeze();
        Registry<SupportAbilityImpl> supportAbilities = new Registry<>("support ability", SupportAbilityImpl::id);
        supportAbilities.register(new SupportAbilityImpl("test:noop", config -> context -> true, definition -> {}));
        Registry<SupportPassiveImpl> supportPassives = new Registry<>("support passive", SupportPassiveImpl::id);
        BattleController battle = new BattleController(config(), critical(), awakening(), abilities, BattleMode.VERSUS,
                List.of(team("test:hero", 200), team(same ? "test:hero" : "test:enemy-high", 300),
                        team(same ? "test:hero" : "test:enemy-low", 150)), List.of(), supportAbilities, supportPassives, new EventBus());
        CoreCombatActions actions = new CoreCombatActions(battle);
        return new Fixture(battle, actions,
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.ONE)),
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.TWO)),
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.THREE)));
    }

    private static BattleTeamDefinition team(String id, float hp) {
        CharacterDefinition main = new CharacterDefinition();
        main.id=id; main.displayGlyph="字"; main.glyphTexture="glyphs/test.png"; main.maxHp=hp; main.baseAttack=1; main.moveSpeed=1; main.collisionRadius=.5f; main.trainingDummy=true;
        SupportCharacterDefinition support = new SupportCharacterDefinition();
        support.id="test:group"; support.displayGlyph="㌠"; support.glyphTexture="glyphs/test.png"; support.projectileTexture="glyphs/test.png";
        support.maxHp=100; support.moveSpeed=8; support.followDistance=2; support.attackRange=30; support.attackInterval=100; support.baseDamage=10; support.projectileSpeed=10;
        support.skill=ability("test:noop-skill", 100); support.ultimate=ability("test:noop-ultimate", 0);
        return new BattleTeamDefinition(main, List.of(), List.of(support));
    }

    private static SupportAbilityDefinition ability(String id, float cooldown) {
        SupportAbilityDefinition value = new SupportAbilityDefinition(); value.id=id; value.type="test:noop"; value.nameKey=id; value.cooldownSeconds=cooldown; value.pointCost=100; return value;
    }

    private static BattleConfig config() {
        BattleConfig value=new BattleConfig(); value.durationSeconds=300; value.fixedStepSeconds=.01f; value.finalMinuteSeconds=60;
        value.finalMinuteAttackMultiplier=1; value.arenaHalfExtent=30; value.groundY=0; value.skillCooldownMultiplier=1; value.chaosHealthMultiplier=1; return value;
    }
    private static CriticalConfig critical() { CriticalConfig value=new CriticalConfig(); value.baseDamageMultiplier=1; return value; }
    private static AwakeningConfig awakening() { AwakeningConfig value=new AwakeningConfig(); value.spawnAtRemainingSeconds=-1; value.attackMultiplier=1; value.defenseMultiplier=1; value.moveSpeedMultiplier=1; value.ultimateGainMultiplier=1; return value; }
    private record Fixture(BattleController battle, CoreCombatActions actions, CombatantView one, CombatantView two, CombatantView three) { }
}
