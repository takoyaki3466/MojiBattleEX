package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.AbilityActivationContext;
import jp.takoa.mojibattle.ability.AbilityImplementationRegistrar;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.ability.DamageEventContext;
import jp.takoa.mojibattle.ability.PassiveRuntime;
import jp.takoa.mojibattle.ability.PassiveUpdateContext;
import jp.takoa.mojibattle.battle.BattlefieldPulseSpec;
import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.CutInSpec;
import jp.takoa.mojibattle.battle.CutInMotion;
import jp.takoa.mojibattle.battle.DamageRequest;
import jp.takoa.mojibattle.battle.DamageType;
import jp.takoa.mojibattle.battle.DomainSpec;
import jp.takoa.mojibattle.battle.DomainVisualStyle;
import jp.takoa.mojibattle.battle.NormalAttackModifier;
import jp.takoa.mojibattle.battle.ProjectileSpec;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 新規メイン4体の、公開APIだけに依存する能力実装。 */
public final class NewCharacterAbilities {
    private static final ResourceId HEAVEN = id("heaven");
    private static final ResourceId SUNRISE = id("sunrise_frontier");

    private NewCharacterAbilities() {
    }

    public static void register(AbilityImplementationRegistrar registrar) {
        registrar.registerSkillImplementation("mojibattle:dimensional_slash", config -> context -> dimensionalSlash(context));
        registrar.registerUltimateImplementation("mojibattle:instant_thousand_slashes", config -> context -> instantThousandSlashes(context));
        registrar.registerPassiveImplementation("mojibattle:heaven_cleaver", config -> new HeavenCleaver());

        registrar.registerSkillImplementation("mojibattle:predatory_pounce", config -> context -> predatoryPounce(context));
        registrar.registerUltimateImplementation("mojibattle:wild_hunt", config -> context -> wildHunt(context));
        registrar.registerPassiveImplementation("mojibattle:feral_evolution", config -> new FeralEvolution());

        registrar.registerSkillImplementation("mojibattle:east_exchange", config -> context -> eastExchange(context));
        registrar.registerUltimateImplementation("mojibattle:sunrise_reversal", config -> context -> sunriseReversal(context));
        registrar.registerPassiveImplementation("mojibattle:east_wind", config -> new EastWind());

        registrar.registerSkillImplementation("mojibattle:sideways_raid", config -> context -> sidewaysRaid(context));
        registrar.registerUltimateImplementation("mojibattle:shell_critical", config -> context -> shellCritical(context));
        registrar.registerPassiveImplementation("mojibattle:molting", config -> new Molting());
    }

    private static boolean dimensionalSlash(AbilityActivationContext context) {
        context.actions().startBattlefieldPulse(context.user(), new BattlefieldPulseSpec(
                4.2f, .42f, 5.8f, DamageRequest.of(9f, DamageType.SKILL),
                id("unit"), 9f));
        context.actions().addUltimatePoints(context.user(), 18f);
        return true;
    }

    private static boolean instantThousandSlashes(AbilityActivationContext context) {
        context.actions().showCutIn(context.user(), new CutInSpec(
                id("cutins/zan-instant-thousand-slashes.png"), "cutin.zan", 2.65f, 100,
                CutInMotion.SLASH_RUSH, 1.35f));
        context.actions().activateDomain(context.user(), new DomainSpec(
                HEAVEN, 16f, DomainVisualStyle.MONOCHROME, 1f, id("unit")));
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().damage(context.user(), enemy, DamageRequest.of(175f, DamageType.ULTIMATE));
        }
        context.actions().emitEffect(context.user(), id("unit"), 24f, 1.2f);
        return true;
    }

    private static final class HeavenCleaver implements PassiveRuntime {
        private int restoredUses;
        private boolean heavenActive;

        @Override
        public void update(PassiveUpdateContext context) {
            heavenActive = context.actions().isDomainActive(context.owner(), HEAVEN);
        }

        @Override
        public boolean ignoresDefense(CombatantView owner) {
            return heavenActive;
        }

        @Override
        public void onUltimateActivated(AbilityActivationContext context) {
            if (context.actions().isDomainActive(context.user(), HEAVEN) && restoredUses < 2) {
                context.actions().refundUltimatePoints(context.user(), context.user().maximumUltimatePoints());
                restoredUses++;
            }
        }

        @Override
        public List<AbilityStateLine> stateLines(CombatantView owner) {
            return List.of(AbilityStateLine.of("state.zan.heaven", 100,
                    heavenActive ? "ON" : "OFF", 2 - restoredUses));
        }
    }

    private static boolean predatoryPounce(AbilityActivationContext context) {
        List<CombatantView> enemies = context.actions().enemiesOf(context.user());
        for (CombatantView enemy : enemies) {
            context.actions().damage(context.user(), enemy, DamageRequest.of(42f, DamageType.SKILL));
            context.actions().knockbackAwayFrom(context.user(), enemy, 2.2f);
        }
        context.actions().healRatio(context.user(), .045f * Math.max(1, enemies.size()));
        context.actions().addUltimatePoints(context.user(), 16f);
        context.actions().emitEffect(context.user(), id("fire"), 8f, .65f);
        return true;
    }

    private static boolean wildHunt(AbilityActivationContext context) {
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().spawnProjectile(context.user(), enemy, new ProjectileSpec(
                    DamageRequest.of(34f, DamageType.ULTIMATE), 22f, 1f, 3.2f,
                    7, 84f, 1, id("glyphs/beast.png"), id("fire")));
        }
        return true;
    }

    /** 与ダメージによって三段階に変異し、通常攻撃の形そのものが変わる。 */
    private static final class FeralEvolution implements PassiveRuntime {
        private float accumulatedDamage;

        @Override
        public void onDamageDealt(DamageEventContext context) {
            accumulatedDamage += context.dealtDamage();
        }

        private int tier() {
            return accumulatedDamage >= 900f ? 3 : accumulatedDamage >= 480f ? 2 : accumulatedDamage >= 180f ? 1 : 0;
        }

        @Override
        public List<NormalAttackModifier> normalAttackModifiers(CombatantView owner) {
            int tier = tier();
            if (tier == 0) return List.of();
            return List.of(new NormalAttackModifier(id("feral_evolution_attack"), 60,
                    1f - tier * .12f, tier, 18f * tier, 0, 1f + tier * .08f,
                    id("glyphs/beast.png"), id("fire")));
        }

        @Override
        public List<AbilityStateLine> stateLines(CombatantView owner) {
            return List.of(AbilityStateLine.of("state.beast.evolution", 80, tier(), Math.round(accumulatedDamage)));
        }
    }

    private static boolean eastExchange(AbilityActivationContext context) {
        if (context.target() != null) {
            context.actions().swapPositions(context.user(), context.target());
        }
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().damage(context.user(), enemy, DamageRequest.of(31f, DamageType.SKILL));
        }
        context.actions().addUltimatePoints(context.user(), 22f);
        context.actions().emitEffect(context.user(), id("star"), 8f, .5f);
        return true;
    }

    private static boolean sunriseReversal(AbilityActivationContext context) {
        context.actions().activateDomain(context.user(), new DomainSpec(
                SUNRISE, 13f, DomainVisualStyle.SUNRISE, .9f, id("star")));
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().mirrorAcrossArenaCenter(enemy);
            context.actions().damage(context.user(), enemy, DamageRequest.of(108f, DamageType.ULTIMATE));
        }
        return true;
    }

    private static final class EastWind implements PassiveRuntime {
        private boolean sunrise;

        @Override
        public void update(PassiveUpdateContext context) {
            sunrise = context.actions().isDomainActive(context.owner(), SUNRISE);
        }

        @Override
        public List<NormalAttackModifier> normalAttackModifiers(CombatantView owner) {
            return sunrise ? List.of(new NormalAttackModifier(id("east_wind_attack"), 70,
                    .42f, 2, 32f, 0, .72f, id("glyphs/east.png"), id("star"))) : List.of();
        }

        @Override
        public List<AbilityStateLine> stateLines(CombatantView owner) {
            return List.of(AbilityStateLine.of("state.east.sunrise", 70, sunrise ? "ON" : "OFF"));
        }
    }

    private static boolean sidewaysRaid(AbilityActivationContext context) {
        if (context.target() != null) {
            context.actions().swapPositions(context.user(), context.target());
        }
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().spawnProjectile(context.user(), enemy, new ProjectileSpec(
                    new DamageRequest(26f, DamageType.SKILL, false, true), 25f, .9f, 2.6f,
                    4, 46f, 1, id("glyphs/crab.png"), id("impact")));
        }
        context.actions().addUltimatePoints(context.user(), 17f);
        return true;
    }

    private static boolean shellCritical(AbilityActivationContext context) {
        context.actions().showCutIn(context.user(), new CutInSpec(
                id("cutins/crab-shell-critical.png"), "cutin.crab", 2.5f, 95,
                CutInMotion.HORIZONTAL_CHARGE, 1.25f));
        for (CombatantView enemy : context.actions().enemiesOf(context.user())) {
            context.actions().spawnProjectile(context.user(), enemy, new ProjectileSpec(
                    new DamageRequest(29f, DamageType.ULTIMATE, false, true), 28f, 1.25f, 3.4f,
                    12, 150f, 1, id("glyphs/crab.png"), id("impact")));
        }
        return true;
    }

    /** 瀕死時に一度だけ脱皮し、回復・浄化後は通常攻撃が三方向になる。 */
    private static final class Molting implements PassiveRuntime {
        private boolean molted;

        @Override
        public void onDamageReceived(DamageEventContext context) {
            if (!molted && context.target().hp() / context.target().maximumHp() <= .35f) {
                molted = true;
                context.actions().healRatio(context.target(), .3f);
                context.actions().cleanseNegativeStatuses(context.target());
                context.actions().emitEffect(context.target(), id("impact"), 12f, 1f);
            }
        }

        @Override
        public List<NormalAttackModifier> normalAttackModifiers(CombatantView owner) {
            return molted ? List.of(new NormalAttackModifier(id("molted_claws"), 90,
                    .72f, 2, 58f, 0, .82f, id("glyphs/crab.png"), id("impact"))) : List.of();
        }

        @Override
        public List<AbilityStateLine> stateLines(CombatantView owner) {
            return List.of(AbilityStateLine.of("state.crab.molting", 90, molted ? "ACTIVE" : "READY"));
        }
    }

    private static ResourceId id(String path) {
        return new ResourceId("mojibattle", path);
    }
}
