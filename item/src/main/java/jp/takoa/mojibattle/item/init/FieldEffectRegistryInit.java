package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.FieldEffectImplementations;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.DeferredRegister;
import jp.takoa.mojibattle.registry.FieldEffectImplementation;
import jp.takoa.mojibattle.registry.RegistryHandle;

/**
 * FieldEffectSystemが解決する計算方式のIDとFactoryだけを宣言する。
 */
public final class FieldEffectRegistryInit {
    public static final DeferredRegister<FieldEffectImplementation> REGISTER = DeferredRegister.create(BuiltInRegistries.FIELD_EFFECT_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<FieldEffectImplementation> CRITICAL_ENABLE = register("critical_enable");
    public static final RegistryHandle<FieldEffectImplementation> COOLDOWN = register("cooldown");
    public static final RegistryHandle<FieldEffectImplementation> MOVE_MULTIPLIER = register("move_multiplier");
    public static final RegistryHandle<FieldEffectImplementation> PERIODIC_HEAL = register("periodic_heal");
    public static final RegistryHandle<FieldEffectImplementation> DAMAGE_TAKEN = register("damage_taken");
    public static final RegistryHandle<FieldEffectImplementation> SPEED_PULSE = register("speed_pulse");
    public static final RegistryHandle<FieldEffectImplementation> ATTACK_GROWTH = register("attack_growth");
    public static final RegistryHandle<FieldEffectImplementation> PROJECTILE_CURVE = register("projectile_curve");
    public static final RegistryHandle<FieldEffectImplementation> TRAFFIC = register("traffic");
    public static final RegistryHandle<FieldEffectImplementation> EXPRESS_MAKI = register("express_maki");
    public static final RegistryHandle<FieldEffectImplementation> CRITICAL_RATE = register("critical_rate");
    public static final RegistryHandle<FieldEffectImplementation> CRITICAL_DAMAGE = register("critical_damage");
    public static final RegistryHandle<FieldEffectImplementation> NORMAL_ATTACK_INTERVAL = register("normal_attack_interval");
    public static final RegistryHandle<FieldEffectImplementation> ATTACK_MULTIPLIER = register("attack_multiplier");
    public static final RegistryHandle<FieldEffectImplementation> INCOMING_DAMAGE_MULTIPLIER = register("incoming_damage_multiplier");

    private FieldEffectRegistryInit() {
    }

    private static RegistryHandle<FieldEffectImplementation> register(String id) {
        return REGISTER.register(id, () -> FieldEffectImplementations.create(id));
    }
}
