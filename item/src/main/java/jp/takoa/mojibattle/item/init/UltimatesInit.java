package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.ability.runtime.UltimateRuntime;
import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.NewCharacterAbilities;
import jp.takoa.mojibattle.item.ability.StandardUltimates;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/** メインキャラクターの必殺技だけを登録する。 */
public final class UltimatesInit {
    public static final MojiBattleRegister.Ultimate REGISTER =
            MojiBattleRegister.createUltimate(ItemContentModule.MOD_ID);

    static {
        register("area", StandardUltimates.Area.class);
        register("self_heal", StandardUltimates.SelfHeal.class);
        register("line", StandardUltimates.Line.class);
        register("meteor_shower", StandardUltimates.MeteorShower.class);
        register("summon_family", StandardUltimates.SummonFamily.class);
        register("moving_car", StandardUltimates.MovingCar.class);
        register("spinning_one", StandardUltimates.SpinningOne.class);
        register("skill_barrage", StandardUltimates.SkillBarrage.class);
        register("sushi_barrage", StandardUltimates.SushiBarrage.class);
        register("cash_crash", StandardUltimates.CashCrash.class);
        NewCharacterAbilities.registerUltimates(REGISTER);
    }

    private UltimatesInit() {
    }

    private static <R extends UltimateRuntime> void register(
            String path, Class<R> runtimeType) {
        REGISTER.registerWithPresentation(path, runtimeType,
                AbilityRegistrationSupport::presentation);
    }
}
