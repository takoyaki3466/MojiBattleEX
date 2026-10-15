package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.SupportImplementations;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.CompatRegister;
import jp.takoa.mojibattle.registry.RegistryHandle;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;

/**
 * 補助能力ハンドラのIDとFactoryだけを宣言する。
 */
public final class SupportRegistryInit {
    public static final CompatRegister<SupportAbilityImplementation> REGISTER = CompatRegister.create(BuiltInRegistries.SUPPORT_ABILITY_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<SupportAbilityImplementation> ALLY_ATTACK_UP = register("ally_attack_up", SupportImplementations::allyAttackUp);
    public static final RegistryHandle<SupportAbilityImplementation> ENEMY_DEFENSE_DOWN = register("enemy_defense_down", SupportImplementations::enemyDefenseDown);
    public static final RegistryHandle<SupportAbilityImplementation> ENEMY_ATTACK_DOWN = register("enemy_attack_down", SupportImplementations::enemyAttackDown);
    public static final RegistryHandle<SupportAbilityImplementation> HEAL_CLEANSE = register("heal_cleanse", SupportImplementations::healCleanse);
    public static final RegistryHandle<SupportAbilityImplementation> BONUS_DAMAGE = register("bonus_damage", SupportImplementations::bonusDamage);
    public static final RegistryHandle<SupportAbilityImplementation> WIDE_SLASH = register("wide_slash", SupportImplementations::wideSlash);
    public static final RegistryHandle<SupportAbilityImplementation> ENEMY_SLOW = register("enemy_slow", SupportImplementations::enemySlow);
    public static final RegistryHandle<SupportAbilityImplementation> RIGHT_TELEPORT = register("right_teleport", SupportImplementations::rightTeleport);
    public static final RegistryHandle<SupportAbilityImplementation> WIND_PULL = register("wind_pull", SupportImplementations::windPull);
    public static final RegistryHandle<SupportAbilityImplementation> GLOBAL_COOLDOWN_HALVE = register("global_cooldown_halve", SupportImplementations::globalCooldownHalve);
    public static final RegistryHandle<SupportAbilityImplementation> TRAIN_CHARGE = register("train_charge", SupportImplementations::trainCharge);
    public static final RegistryHandle<SupportAbilityImplementation> GATHER_ALL = register("gather_all", SupportImplementations::gatherAll);

    private SupportRegistryInit() {
    }

    private static RegistryHandle<SupportAbilityImplementation> register(String id, java.util.function.Supplier<SupportAbilityImplementation> factory) {
        return REGISTER.register(id, factory);
    }
}
