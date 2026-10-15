package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.IUltimate;
import jp.takoa.mojibattle.item.impl.ability.*;
import jp.takoa.mojibattle.registry.AbilityPresentation;
import jp.takoa.mojibattle.registry.UltimateImplementation;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 必殺技の実行Factory、固有検証およびガイド表示定義を保持する実装カタログ。
 */
public final class UltimateImplementations {
    private static final float DAMAGE_SCALE = .65f;

    private UltimateImplementations() {
    }

    public static UltimateImplementation area() {
        return create("area", BankruptcyUltimate::new, c -> require(c.radius > 0, "Area ultimate radius is invalid: " + c.id), c -> AbilityPresentation.of("guide.area", Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.radius * 5f)));
    }

    public static UltimateImplementation selfHeal() {
        return create("self_heal", SelfHealUltimate::new, c -> require(c.healAmount > 0, "Heal ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.heal", Math.round(c.healAmount)));
    }

    public static UltimateImplementation line() {
        return create("line", LineUltimate::new, c -> require(c.lineLength > 0 && c.lineWidth > 0, "Line ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.line", Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.lineLength), decimal(c.lineWidth * 5.2f), Math.max(1, c.directions)));
    }

    public static UltimateImplementation meteorShower() {
        return create("meteor_shower", MeteorShowerUltimate::new, c -> require(c.meteorCount > 0 && c.radius > 0, "Meteor ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.meteor", c.meteorCount, Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.radius * 5f)));
    }

    public static UltimateImplementation summonFamily() {
        return create("summon_family", FamilyUltimate::new, c -> require(c.summonCount > 0 && c.summonHp > 0 && c.summonDefense >= 0 && c.summonAttackDamage > 0 && c.summonAttackInterval > 0 && c.summonProjectileSpeed > 0, "Family ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.family_ultimate", c.summonCount, Math.round(c.summonHp), decimal(c.summonAttackDamage), decimal(c.summonDefense)));
    }

    public static UltimateImplementation movingCar() {
        return create("moving_car", MovingCarUltimate::new, c -> require(c.radius > 0 && c.movementSpeed > 0 && c.stunSeconds > 0 && c.effectSeconds > 0, "Moving-car ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.car", Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.effectSeconds), decimal(c.stunSeconds)));
    }

    public static UltimateImplementation spinningOne() {
        return create("spinning_one", SpinningOneUltimate::new, c -> require(c.lineLength > 0 && c.lineWidth > 0 && c.healAmount > 0 && c.effectSeconds > 0, "Spinning-one ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.spinning_one", Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.lineLength), Math.round(c.healAmount)));
    }

    public static UltimateImplementation skillBarrage() {
        return create("skill_barrage", SkillBarrageUltimate::new, c -> require(c.barrageCount > 0 && c.barrageInterval > 0 && c.barrageSkillId != null, "Skill-barrage ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.barrage", c.barrageCount, decimal(c.barrageInterval)));
    }

    public static UltimateImplementation sushiBarrage() {
        return create("sushi_barrage", SushiBombingUltimate::new, c -> require(c.barrageCount > 0 && c.barrageInterval > 0 && c.projectileSpeed > 0 && c.projectileLifetimeSeconds > 0 && c.projectileRadius > 0, "Sushi-barrage ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.sushi_barrage", c.barrageCount, Math.round(c.baseDamage), decimal(c.barrageInterval), decimal(c.projectileSpeed)));
    }

    public static UltimateImplementation cashCrash() {
        return create("cash_crash", CashCrashUltimate::new, c -> require(c.baseDamage > 0 && c.radius > 0 && c.defenseDownMultiplier > 1 && c.defenseDownDuration > 0, "Cash-crash ultimate config is invalid: " + c.id), c -> AbilityPresentation.of("guide.cash_crash", Math.round(c.baseDamage * DAMAGE_SCALE), decimal(c.radius * 5f), Math.round((c.defenseDownMultiplier - 1f) * 100), decimal(c.defenseDownDuration)));
    }

    private static UltimateImplementation create(String id, Function<AbilityConfigs.AreaUltimate, IUltimate> factory, Consumer<AbilityConfigs.AreaUltimate> validator, Function<AbilityConfigs.AreaUltimate, AbilityPresentation> presentation) {
        return new UltimateImplementation(id, factory, validator, presentation);
    }

    private static String decimal(float value) {
        return String.format("%.1f", value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
