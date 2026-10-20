package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.FieldEffectImplementations;
import jp.takoa.mojibattle.registry.MojiBattleRegistries;
import jp.takoa.mojibattle.registry.MojiBattleRegister;
import jp.takoa.mojibattle.registry.impl.FieldEffectImpl;
import jp.takoa.mojibattle.registry.RegistryHandle;

/**
 * FieldEffectSystemが解決する計算方式のIDとFactoryだけを宣言する。
 */
public final class FieldEffectRegistryInit {
    public static final MojiBattleRegister<FieldEffectImpl> REGISTER = MojiBattleRegister.create(MojiBattleRegistries.FIELD_EFFECT_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<FieldEffectImpl> CRITICAL_ENABLE = register("critical_enable");
    public static final RegistryHandle<FieldEffectImpl> COOLDOWN = register("cooldown");
    public static final RegistryHandle<FieldEffectImpl> MOVE_MULTIPLIER = register("move_multiplier");
    public static final RegistryHandle<FieldEffectImpl> PERIODIC_HEAL = register("periodic_heal");
    public static final RegistryHandle<FieldEffectImpl> DAMAGE_TAKEN = register("damage_taken");
    public static final RegistryHandle<FieldEffectImpl> SPEED_PULSE = register("speed_pulse");
    public static final RegistryHandle<FieldEffectImpl> ATTACK_GROWTH = register("attack_growth");
    public static final RegistryHandle<FieldEffectImpl> PROJECTILE_CURVE = register("projectile_curve");
    public static final RegistryHandle<FieldEffectImpl> TRAFFIC = register("traffic");
    public static final RegistryHandle<FieldEffectImpl> EXPRESS_MAKI = register("express_maki");
    public static final RegistryHandle<FieldEffectImpl> CRITICAL_RATE = register("critical_rate");
    public static final RegistryHandle<FieldEffectImpl> CRITICAL_DAMAGE = register("critical_damage");
    public static final RegistryHandle<FieldEffectImpl> NORMAL_ATTACK_INTERVAL = register("normal_attack_interval");
    public static final RegistryHandle<FieldEffectImpl> ATTACK_MULTIPLIER = register("attack_multiplier");
    public static final RegistryHandle<FieldEffectImpl> INCOMING_DAMAGE_MULTIPLIER = register("incoming_damage_multiplier");

    private FieldEffectRegistryInit() {
    }

    private static RegistryHandle<FieldEffectImpl> register(String id) {
        return REGISTER.register(id, () -> FieldEffectImplementations.create(id));
    }
}
