package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.registry.MojiBattleRegistries;
import jp.takoa.mojibattle.registry.CombatEffectDefinition;
import jp.takoa.mojibattle.registry.MojiBattleRegister;
import jp.takoa.mojibattle.registry.RegistryHandle;

import java.util.Set;

/**
 * 旧0～15番VFXを安定した名前空間IDへ結び付ける登録宣言。
 */
public final class EffectRegistryInit {
    public static final MojiBattleRegister<CombatEffectDefinition> REGISTER = MojiBattleRegister.create(MojiBattleRegistries.EFFECTS, "mojibattle");

    public static final RegistryHandle<CombatEffectDefinition> NEUTRAL = effect("neutral", 0);
    public static final RegistryHandle<CombatEffectDefinition> STOCK = effect("stock", 1);
    public static final RegistryHandle<CombatEffectDefinition> FORMULA = effect("formula", 2);
    public static final RegistryHandle<CombatEffectDefinition> SPEED = effect("speed", 3);
    public static final RegistryHandle<CombatEffectDefinition> FIRE = effect("fire", 4);
    public static final RegistryHandle<CombatEffectDefinition> IMPACT = effect("impact", 5);
    public static final RegistryHandle<CombatEffectDefinition> WIND = effect("wind", 6);
    public static final RegistryHandle<CombatEffectDefinition> KANA = effect("kana", 7);
    public static final RegistryHandle<CombatEffectDefinition> STUN = effect("stun", 8);
    public static final RegistryHandle<CombatEffectDefinition> UNIT = effect("unit", 9);
    public static final RegistryHandle<CombatEffectDefinition> METEOR = effect("meteor", 10);
    public static final RegistryHandle<CombatEffectDefinition> VEHICLE = effect("vehicle", 11);
    public static final RegistryHandle<CombatEffectDefinition> CRITICAL = effect("critical", 12);
    public static final RegistryHandle<CombatEffectDefinition> CASH = effect("cash", 13);
    public static final RegistryHandle<CombatEffectDefinition> STAR = effect("star", 14);
    public static final RegistryHandle<CombatEffectDefinition> FAMILY = effect("family", 15);


    private EffectRegistryInit() {
    }

    private static RegistryHandle<CombatEffectDefinition> effect(String id, int legacyIndex) {
        return REGISTER.register(id, () -> new CombatEffectDefinition("mojibattle:" + id, "vfx/effect-%02d.png".formatted(legacyIndex), 1f, 1f, "pulse_rotate", 16f, 280f, Set.of(CombatEffectDefinition.Usage.values()), legacyIndex));
    }
}
