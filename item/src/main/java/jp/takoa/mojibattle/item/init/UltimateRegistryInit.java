package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.UltimateImplementations;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.CompatRegister;
import jp.takoa.mojibattle.registry.RegistryHandle;
import jp.takoa.mojibattle.registry.UltimateImplementation;

/**
 * 必殺技実装の登録IDと遅延生成Factoryだけを宣言する。
 */
public final class UltimateRegistryInit {
    public static final CompatRegister<UltimateImplementation> REGISTER = CompatRegister.create(BuiltInRegistries.ULTIMATE_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<UltimateImplementation> AREA = register("area", UltimateImplementations::area);
    public static final RegistryHandle<UltimateImplementation> SELF_HEAL = register("self_heal", UltimateImplementations::selfHeal);
    public static final RegistryHandle<UltimateImplementation> LINE = register("line", UltimateImplementations::line);
    public static final RegistryHandle<UltimateImplementation> METEOR_SHOWER = register("meteor_shower", UltimateImplementations::meteorShower);
    public static final RegistryHandle<UltimateImplementation> SUMMON_FAMILY = register("summon_family", UltimateImplementations::summonFamily);
    public static final RegistryHandle<UltimateImplementation> MOVING_CAR = register("moving_car", UltimateImplementations::movingCar);
    public static final RegistryHandle<UltimateImplementation> SPINNING_ONE = register("spinning_one", UltimateImplementations::spinningOne);
    public static final RegistryHandle<UltimateImplementation> SKILL_BARRAGE = register("skill_barrage", UltimateImplementations::skillBarrage);
    public static final RegistryHandle<UltimateImplementation> SUSHI_BARRAGE = register("sushi_barrage", UltimateImplementations::sushiBarrage);
    public static final RegistryHandle<UltimateImplementation> CASH_CRASH = register("cash_crash", UltimateImplementations::cashCrash);

    private UltimateRegistryInit() {
    }

    private static RegistryHandle<UltimateImplementation> register(String id, java.util.function.Supplier<UltimateImplementation> factory) {
        return REGISTER.register(id, factory);
    }
}
