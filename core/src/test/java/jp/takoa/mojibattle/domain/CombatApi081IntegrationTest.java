package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.support.SupportSplitMemberSpec;
import jp.takoa.mojibattle.ability.support.SupportSplitSpec;
import jp.takoa.mojibattle.ability.context.PassiveUpdateContext;
import jp.takoa.mojibattle.battle.action.*;
import jp.takoa.mojibattle.battle.action.spec.*;
import jp.takoa.mojibattle.battle.combatant.*;
import jp.takoa.mojibattle.battle.damage.*;
import jp.takoa.mojibattle.battle.modifier.SupportNormalAttackModifier;
import jp.takoa.mojibattle.battle.presentation.*;
import jp.takoa.mojibattle.battle.rule.*;
import jp.takoa.mojibattle.battle.status.*;
import jp.takoa.mojibattle.config.*;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatActions;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatantView;
import jp.takoa.mojibattle.domain.ability.adapter.CoreUnifiedCombatantView;
import jp.takoa.mojibattle.domain.combat.TemporarySupportSummon;
import jp.takoa.mojibattle.domain.combat.FamilySummon;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.support.*;
import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.input.PlayerCommand;
import jp.takoa.mojibattle.item.ability.NewSupportAbilities;
import jp.takoa.mojibattle.item.ability.StandardPassives;
import jp.takoa.mojibattle.item.ability.skill.character.PredatoryPounceSkill;
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

    @Test void statusTargetSelectionExcludesUnsupportedFamilySummons() {
        Fixture f = fixture();
        FamilySummon family = new FamilySummon(PlayerId.TWO, new com.badlogic.gdx.math.Vector3(2, 0, 2),
                "glyphs/test.png", 100, 0, 1, 10, 10, "test:impact", 0, 2);
        f.battle.spawn(family);

        List<CombatantView> allEnemies = f.actions.enemiesOf(f.one);
        CombatantView familyView = allEnemies.stream()
                .filter(value -> value.id().equals("mojibattle:family_summon")).findFirst().orElseThrow();
        List<CombatantView> statusTargets = f.actions.statusEffectEnemiesOf(f.one);

        assertFalse(f.actions.canApplyStatus(familyView));
        assertFalse(statusTargets.stream().anyMatch(value -> value.instanceId().equals(familyView.instanceId())));
        assertTrue(statusTargets.stream().anyMatch(value -> value.kind() == CombatantKind.MAIN));
        assertTrue(statusTargets.stream().anyMatch(value -> value.kind() == CombatantKind.SUPPORT));
        assertThrows(IllegalArgumentException.class, () -> f.actions.applyStatus(f.one, familyView,
                StatusSpec.multiplier(ResourceId.parse("test:invalid-family-status"),
                        StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(1))));

        for (CombatantView target : statusTargets) {
            f.actions.applyStatus(f.one, target, StatusSpec.multiplier(ResourceId.parse("test:attack-down"),
                    StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(.1f)));
        }
        assertEquals(.5f, f.battle.statusesOf(f.battle.player(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);
        assertEquals(.5f, f.battle.statusesOf(f.battle.support(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);

        f.battle.update(.2f, commands());
        assertEquals(1f, f.battle.statusesOf(f.battle.player(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);
        assertEquals(1f, f.battle.statusesOf(f.battle.support(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);
    }

    @Test void deadAndRemovedCombatantsRejectStatusApplication() {
        Fixture f = fixture();
        CombatantView support = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.kind() == CombatantKind.SUPPORT).findFirst().orElseThrow();
        f.actions.damage(f.one, support, DamageRequest.of(support.maximumHp() * 10, DamageType.NORMAL));

        assertFalse(support.alive());
        assertFalse(f.actions.statusEffectEnemiesOf(f.one).stream()
                .anyMatch(value -> value.instanceId().equals(support.instanceId())));
        assertThrows(IllegalArgumentException.class, () -> f.actions.applyStatus(f.one, support,
                StatusSpec.multiplier(ResourceId.parse("test:dead-status"),
                        StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(1))));

        TemporarySupportSummon temporary = new TemporarySupportSummon(PlayerId.TWO, "two:support:0",
                "two:support:0:split:test", "test:group", "㌠", "glyphs/test.png",
                new com.badlogic.gdx.math.Vector3(2, 0, 2), 100, 1, 10, 1, null, .1f, false);
        f.battle.spawn(temporary);
        CombatantView temporaryView = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.instanceId().equals(temporary.instanceId)).findFirst().orElseThrow();
        assertTrue(f.actions.canApplyStatus(temporaryView));
        f.battle.update(.01f, commands());
        f.battle.update(.2f, commands());
        assertFalse(f.actions.canApplyStatus(temporaryView));
        assertThrows(IllegalArgumentException.class, () -> f.actions.applyStatus(f.one, temporaryView,
                StatusSpec.multiplier(ResourceId.parse("test:removed-status"),
                        StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(1))));
    }

    @Test void statusTargetSelectionSupportsMultipleSupportsAndMainOnlyTeams() {
        Fixture multiple = fixture(List.of(team("test:hero", 200, 1), team("test:enemy-high", 300, 2),
                team("test:enemy-low", 150, 0)));
        List<CombatantView> multipleTargets = multiple.actions.statusEffectEnemiesOf(multiple.one);
        assertEquals(2, multipleTargets.stream().filter(value -> value.kind() == CombatantKind.SUPPORT).count());
        for (CombatantView target : multipleTargets) {
            multiple.actions.applyStatus(multiple.one, target, StatusSpec.multiplier(
                    ResourceId.parse("test:multi-support-status"), StatusEffectType.ATTACK_MULTIPLIER,
                    .5f, StatusDuration.seconds(1)));
        }
        for (SupportCharacterRuntime support : multiple.battle.supports(PlayerId.TWO)) {
            assertEquals(.5f, support.statuses.multiplier(StatusType.ATTACK_DOWN), .0001f);
        }

        Fixture mainOnly = fixture(List.of(team("test:hero", 200, 0), team("test:enemy-high", 300, 0),
                team("test:enemy-low", 150, 0)));
        List<CombatantView> mainTargets = mainOnly.actions.statusEffectEnemiesOf(mainOnly.one);
        assertEquals(2, mainTargets.size());
        assertTrue(mainTargets.stream().allMatch(value -> value.kind() == CombatantKind.MAIN));
        assertDoesNotThrow(() -> mainTargets.forEach(target -> mainOnly.actions.applyStatus(mainOnly.one, target,
                StatusSpec.multiplier(ResourceId.parse("test:main-only-status"),
                        StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(1)))));
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

    @Test void unifiedMainSupportsActionLocksAndDifficultKanjiSelectsOnlyCapableTargets() {
        Fixture f = fixture();
        CombatantView enemySupport = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.kind() == CombatantKind.SUPPORT).findFirst().orElseThrow();
        FamilySummon family = new FamilySummon(PlayerId.TWO, new com.badlogic.gdx.math.Vector3(2, 0, 2),
                "glyphs/test.png", 100, 0, 1, 10, 10, "test:impact", 0, 2);
        f.battle.spawn(family);
        CombatantView familyView = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.id().equals("mojibattle:family_summon")).findFirst().orElseThrow();

        assertTrue(f.actions.canStun(f.two));
        assertTrue(f.actions.canBind(f.two));
        assertFalse(f.actions.canStun(enemySupport));
        assertFalse(f.actions.canBind(familyView));
        assertThrows(IllegalArgumentException.class, () -> f.actions.stun(enemySupport, .1f));
        assertThrows(IllegalArgumentException.class, () -> f.actions.bind(familyView, .1f));

        StandardPassives.DifficultKanji passive = new StandardPassives.DifficultKanji(
                new StandardPassives.DifficultKanji.Config(1f, 1f, .1f, 0f));
        assertDoesNotThrow(() -> passive.update(new PassiveUpdateContext(f.one, f.actions, .01f, 0f)));
        assertTrue(f.battle.player(PlayerId.TWO).isStunned());
        assertFalse(f.battle.player(PlayerId.THREE).isStunned());

        f.battle.update(.2f, commands());
        assertFalse(f.battle.player(PlayerId.TWO).isStunned());
        f.actions.bind(f.two, .1f);
        assertTrue(f.battle.player(PlayerId.TWO).movementLocked());
        f.battle.update(.2f, commands());
        assertFalse(f.battle.player(PlayerId.TWO).movementLocked());
    }

    @Test void resolverRejectsForeignRemovedAndClosedBattleCombatants() {
        Fixture f = fixture();
        Fixture foreign = fixture();
        assertFalse(f.actions.canApplyStatus(foreign.two));
        assertFalse(f.actions.canStun(foreign.two));
        assertThrows(IllegalArgumentException.class, () -> f.actions.damage(f.one, foreign.two, 1));

        TemporarySupportSummon temporary = new TemporarySupportSummon(PlayerId.TWO, "two:support:0",
                "two:support:0:split:resolver", "test:group", "㌠", "glyphs/test.png",
                new com.badlogic.gdx.math.Vector3(2, 0, 2), 100, 1, 10, 1, null, .1f, false);
        f.battle.spawn(temporary);
        CombatantView temporaryView = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.instanceId().equals(temporary.instanceId)).findFirst().orElseThrow();
        f.battle.update(.01f, commands());
        f.battle.update(.2f, commands());
        assertThrows(IllegalArgumentException.class, () -> f.actions.enemiesOf(temporaryView));

        f.battle.close();
        assertFalse(f.actions.canStun(f.two));
        assertThrows(IllegalArgumentException.class, () -> f.actions.enemiesOf(f.one));
    }

    @Test void genericMovementCleanseAndUltimatePointsFollowCombatantCapabilities() {
        Fixture f = fixture();
        CombatantView support = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.kind() == CombatantKind.SUPPORT).findFirst().orElseThrow();
        float distanceBefore = distance(support.position(), f.one.position());
        f.actions.pullToward(support, f.one, 4f, 0f);
        assertTrue(distance(support.position(), f.one.position()) < distanceBefore);

        f.actions.applyStatus(f.one, support, StatusSpec.multiplier(ResourceId.parse("test:cleanse"),
                StatusEffectType.ATTACK_MULTIPLIER, .5f, StatusDuration.seconds(3)));
        assertEquals(.5f, f.battle.statusesOf(f.battle.support(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);
        f.actions.cleanseNegativeStatuses(support);
        assertEquals(1f, f.battle.statusesOf(f.battle.support(PlayerId.TWO))
                .multiplier(StatusType.ATTACK_DOWN), .0001f);

        f.actions.stun(f.two, 1f);
        f.actions.bind(f.two, 1f);
        f.actions.cleanseNegativeStatuses(f.two);
        assertFalse(f.battle.player(PlayerId.TWO).isStunned());
        assertFalse(f.battle.player(PlayerId.TWO).movementLocked());

        f.actions.addUltimatePoints(support, 12f);
        assertEquals(12f, support.ultimatePoints(), .0001f);
        FamilySummon family = new FamilySummon(PlayerId.TWO, new com.badlogic.gdx.math.Vector3(2, 0, 2),
                "glyphs/test.png", 100, 0, 1, 10, 10, "test:impact", 0, 2);
        f.battle.spawn(family);
        CombatantView familyView = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.id().equals("mojibattle:family_summon")).findFirst().orElseThrow();
        float familyDistanceBefore = distance(familyView.position(), f.one.position());
        f.actions.pullToward(familyView, f.one, 1f, 0f);
        assertTrue(distance(familyView.position(), f.one.position()) < familyDistanceBefore);
        assertThrows(IllegalArgumentException.class, () -> f.actions.addUltimatePoints(familyView, 1f));
        assertThrows(IllegalArgumentException.class, () -> f.actions.cleanseNegativeStatuses(familyView));

        TemporarySupportSummon temporary = new TemporarySupportSummon(PlayerId.TWO, "two:support:0",
                "two:support:0:split:cleanse", "test:group", "㌠", "glyphs/test.png",
                new com.badlogic.gdx.math.Vector3(2, 0, 2), 100, 1, 10, 1, null, 3f, false);
        f.battle.spawn(temporary);
        CombatantView temporaryView = f.actions.enemiesOf(f.one).stream()
                .filter(value -> value.instanceId().equals(temporary.instanceId)).findFirst().orElseThrow();
        f.actions.applyStatus(f.one, temporaryView, StatusSpec.multiplier(ResourceId.parse("test:temporary-cleanse"),
                StatusEffectType.MOVE_SPEED_MULTIPLIER, .5f, StatusDuration.seconds(3)));
        f.actions.cleanseNegativeStatuses(temporaryView);
        assertEquals(1f, temporary.statuses.multiplier(StatusType.MOVE_DOWN), .0001f);
    }

    @Test void predatoryPounceKeepsDamageThenKnockbackValidWhenTargetsDie() {
        SupportCharacterDefinition flower = support("flower");
        SupportCharacterDefinition eyebrow = support("expansion_mod:eyebrow");
        eyebrow.maxHp = 1f;
        Fixture f = fixture(List.of(
                team("mojibattle:beast", 500, List.of(flower)),
                team("mojibattle:biang", 1, List.of(eyebrow)),
                team("test:enemy", 1, List.of())));
        FamilySummon family = new FamilySummon(PlayerId.TWO, new com.badlogic.gdx.math.Vector3(2, 0, 2),
                "glyphs/test.png", 1, 0, 1, 10, 10, "test:impact", 0, 2);
        TemporarySupportSummon temporary = new TemporarySupportSummon(PlayerId.TWO, "two:support:0",
                "two:support:0:split:pounce", "expansion_mod:eyebrow", "眉", "glyphs/test.png",
                new com.badlogic.gdx.math.Vector3(3, 0, 2), 1, 1, 10, 1, null, 3f, false);
        f.battle.spawn(family);
        f.battle.spawn(temporary);
        List<CombatantView> targets = f.actions.enemiesOf(f.one);

        PredatoryPounceSkill skill = new PredatoryPounceSkill(new PredatoryPounceSkill.Config());
        assertDoesNotThrow(() -> skill.tryActivate(new AbilityActivationContext(f.one, f.two, f.actions)));
        assertTrue(targets.stream().noneMatch(CombatantView::alive));

        assertDoesNotThrow(() -> f.actions.knockbackAwayFrom(f.one, targets.getFirst(), 2.2f));
        f.battle.update(.01f, commands());
        assertThrows(IllegalArgumentException.class,
                () -> f.actions.knockbackAwayFrom(f.one, targets.getFirst(), 2.2f));
    }

    @Test void predatoryPounceKnocksBackEachLivingCombatantKind() {
        Fixture f = fixture(List.of(
                team("mojibattle:beast", 500, List.of(support("flower"))),
                team("mojibattle:biang", 500, List.of(support("expansion_mod:eyebrow"))),
                team("test:enemy", 500, List.of())));
        f.battle.spawn(new FamilySummon(PlayerId.TWO, new com.badlogic.gdx.math.Vector3(2, 0, 2),
                "glyphs/test.png", 100, 0, 1, 10, 10, "test:impact", 0, 2));
        f.battle.spawn(new TemporarySupportSummon(PlayerId.TWO, "two:support:0",
                "two:support:0:split:pounce-living", "expansion_mod:eyebrow", "眉", "glyphs/test.png",
                new com.badlogic.gdx.math.Vector3(3, 0, 2), 100, 1, 10, 1, null, 3f, false));
        List<CombatantView> targets = f.actions.enemiesOf(f.one);
        java.util.Map<String, CombatantPosition> before = targets.stream().collect(
                java.util.stream.Collectors.toMap(CombatantView::instanceId, CombatantView::position));

        PredatoryPounceSkill skill = new PredatoryPounceSkill(new PredatoryPounceSkill.Config());
        assertTrue(skill.tryActivate(new AbilityActivationContext(f.one, f.two, f.actions)));

        for (CombatantView target : targets) {
            assertTrue(distance(before.get(target.instanceId()), target.position()) > .01f,
                    () -> "Target was not knocked back: " + target.instanceId());
        }
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
        assertEquals(ResourceId.parse("mojibattle:glyphs/test.png"), f.battle.cutIns().getFirst().spec().image());
    }

    @Test void supportCutInKeepsTheSupportImageNamespaceIndependentFromTheMainCharacter() {
        Fixture f = fixture(List.of(team("expansion_mod:sigma", 200), team("test:enemy-high", 300),
                team("test:enemy-low", 150)));
        SupportCharacterRuntime south = f.battle.support(PlayerId.ONE);
        south.definition.id = "south";
        south.definition.glyphTexture = "glyphs/south.png";
        south.maximizeUltimate();
        Registry<SupportAbilityImpl> abilities = new Registry<>("support ability", SupportAbilityImpl::id);
        abilities.register(new SupportAbilityImpl("test:noop", config -> context -> true, definition -> {}));

        assertTrue(new SupportAbilityService(abilities).useUltimate(
                f.battle, f.battle.team(PlayerId.ONE), south));

        assertEquals(ResourceId.parse("mojibattle:glyphs/south.png"),
                f.battle.cutIns().getFirst().spec().image());
    }

    @Test void floweringCycleAddsMaintainsAndRemovesItsModifierPerSupportInstance() {
        SupportCharacterDefinition flower = flower();
        Fixture f = fixture(List.of(
                        team("mojibattle:biang", 200, List.of(flower, flower)),
                        team("test:enemy-high", 300, List.of()),
                        team("test:enemy-low", 150, List.of())),
                standardSupportPassives());
        SupportCharacterRuntime first = f.battle.supports(PlayerId.ONE).get(0);
        SupportCharacterRuntime second = f.battle.supports(PlayerId.ONE).get(1);
        assertTrue(first.normalAttackModifiers().isEmpty());
        assertTrue(second.normalAttackModifiers().isEmpty());

        ResourceId domainId = ResourceId.parse("mojibattle:everbloom_garden");
        f.battle.activateDomain(f.battle.player(PlayerId.ONE), new DomainSpec(
                domainId, .1f, DomainVisualStyle.BLOOM, .9f, null));
        f.battle.update(.01f, commands());

        var modifier = first.normalAttackModifiers().getFirst();
        assertEquals(SupportNormalAttackModifier.Stacking.ADD, modifier.stacking());
        assertEquals(-.15f, modifier.damageMultiplier(), .0001f);
        assertEquals(-.35f, modifier.intervalMultiplier(), .0001f);
        assertEquals(4, modifier.projectileCount());
        assertEquals(0, modifier.hitsPerAttack());
        assertEquals(120f, modifier.spreadAngle(), .0001f);
        assertEquals(1, second.normalAttackModifiers().size());

        f.battle.update(.01f, commands());
        assertEquals(1, first.normalAttackModifiers().size());
        assertEquals(1, second.normalAttackModifiers().size());

        f.battle.update(.1f, commands());
        assertTrue(first.normalAttackModifiers().isEmpty());
        assertTrue(second.normalAttackModifiers().isEmpty());
    }

    private static EnumMap<PlayerId, PlayerCommand> commands() { return new EnumMap<>(PlayerId.class); }

    private static float distance(CombatantPosition first, CombatantPosition second) {
        float x = first.x() - second.x();
        float z = first.z() - second.z();
        return (float) Math.sqrt(x * x + z * z);
    }

    private static Fixture fixture() { return fixture(false); }
    private static Fixture fixtureSameDefinitions() { return fixture(true); }
    private static Fixture fixture(boolean same) {
        return fixture(List.of(team("test:hero", 200), team(same ? "test:hero" : "test:enemy-high", 300),
                team(same ? "test:hero" : "test:enemy-low", 150)));
    }

    private static Fixture fixture(List<BattleTeamDefinition> teams) {
        return fixture(teams, new Registry<>("support passive", SupportPassiveImpl::id));
    }

    private static Fixture fixture(List<BattleTeamDefinition> teams,
                                   Registry<SupportPassiveImpl> supportPassives) {
        AbilityRegistry abilities = new AbilityRegistry(); abilities.freeze();
        Registry<SupportAbilityImpl> supportAbilities = new Registry<>("support ability", SupportAbilityImpl::id);
        supportAbilities.register(new SupportAbilityImpl("test:noop", config -> context -> true, definition -> {}));
        BattleController battle = new BattleController(config(), critical(), awakening(), abilities, BattleMode.VERSUS,
                teams, List.of(), supportAbilities, supportPassives, new EventBus());
        CoreCombatActions actions = new CoreCombatActions(battle);
        return new Fixture(battle, actions,
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.ONE)),
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.TWO)),
                new CoreUnifiedCombatantView(battle, battle.player(PlayerId.THREE)));
    }

    private static BattleTeamDefinition team(String id, float hp) {
        return team(id, hp, 1);
    }

    private static BattleTeamDefinition team(String id, float hp, int supportCount) {
        SupportCharacterDefinition support = support("test:group");
        return team(id, hp, java.util.Collections.nCopies(supportCount, support));
    }

    private static BattleTeamDefinition team(String id, float hp, List<SupportCharacterDefinition> supports) {
        CharacterDefinition main = new CharacterDefinition();
        main.id=id; main.displayGlyph="字"; main.glyphTexture="glyphs/test.png"; main.maxHp=hp; main.baseAttack=1; main.moveSpeed=1; main.collisionRadius=.5f; main.trainingDummy=true;
        return new BattleTeamDefinition(main, List.of(), supports);
    }

    private static SupportCharacterDefinition support(String id) {
        SupportCharacterDefinition support = new SupportCharacterDefinition();
        support.id=id; support.displayGlyph="㌠"; support.glyphTexture="glyphs/test.png"; support.projectileTexture="glyphs/test.png";
        support.maxHp=100; support.moveSpeed=8; support.followDistance=2; support.attackRange=30; support.attackInterval=100; support.baseDamage=10; support.projectileSpeed=10;
        support.skill=ability("test:noop-skill", 100); support.ultimate=ability("test:noop-ultimate", 0);
        return support;
    }

    private static SupportCharacterDefinition flower() {
        SupportCharacterDefinition flower = support("flower");
        flower.supportPassiveImplementationId = "mojibattle:flowering_cycle";
        return flower;
    }

    private static Registry<SupportPassiveImpl> standardSupportPassives() {
        MojiBattleRegister.SupportPassive deferred = MojiBattleRegister.createSupportPassive("mojibattle");
        NewSupportAbilities.registerPassives(deferred);
        RegistryManager manager = new RegistryManager();
        Registry<SupportPassiveImpl> registry = manager.create(
                MojiBattleRegistries.SUPPORT_PASSIVE_IMPLEMENTATIONS,
                "support passive implementation", SupportPassiveImpl::id);
        EventBus lifecycle = new EventBus();
        deferred.attach(lifecycle);
        manager.publishRegistrationEvents(lifecycle);
        manager.freezeAll();
        lifecycle.close();
        return registry;
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
