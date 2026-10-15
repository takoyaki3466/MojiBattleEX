package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.PassiveImplementations;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.CompatRegister;
import jp.takoa.mojibattle.registry.PassiveImplementation;
import jp.takoa.mojibattle.registry.RegistryHandle;

/**
 * パッシブ実装のIDと遅延生成Factoryだけを宣言する。
 */
public final class PassiveRegistryInit {
    public static final CompatRegister<PassiveImplementation> REGISTER = CompatRegister.create(BuiltInRegistries.PASSIVE_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<PassiveImplementation> COMPOUND_INTEREST = standard("compound_interest");
    public static final RegistryHandle<PassiveImplementation> HITBOX_BOOST = standard("hitbox_boost");
    public static final RegistryHandle<PassiveImplementation> ULTIMATE_REGEN = standard("ultimate_regen");
    public static final RegistryHandle<PassiveImplementation> DAMAGE_GUARD = standard("damage_guard");
    public static final RegistryHandle<PassiveImplementation> SPEED_BOOST = standard("speed_boost");
    public static final RegistryHandle<PassiveImplementation> ULTIMATE_GAIN = standard("ultimate_gain");
    public static final RegistryHandle<PassiveImplementation> STUN_BOOST = standard("stun_boost");
    public static final RegistryHandle<PassiveImplementation> SKILL_HASTE = standard("skill_haste");
    public static final RegistryHandle<PassiveImplementation> FAMILY_BOND = standard("family_bond");
    public static final RegistryHandle<PassiveImplementation> DIFFICULT_KANJI = REGISTER.register("difficult_kanji", PassiveImplementations::difficultKanji);
    public static final RegistryHandle<PassiveImplementation> BERSERKER = REGISTER.register("berserker", PassiveImplementations::berserker);
    public static final RegistryHandle<PassiveImplementation> ULTIMATE_BOOST = REGISTER.register("ultimate_boost", PassiveImplementations::ultimateBoost);
    public static final RegistryHandle<PassiveImplementation> CASH_SCATTER = REGISTER.register("cash_scatter", PassiveImplementations::cashScatter);

    private PassiveRegistryInit() {
    }

    private static RegistryHandle<PassiveImplementation> standard(String id) {
        return REGISTER.register(id, () -> PassiveImplementations.standard(id));
    }
}
