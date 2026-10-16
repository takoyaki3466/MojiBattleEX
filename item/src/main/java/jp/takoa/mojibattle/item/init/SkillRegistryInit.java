package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.impl.registry.SkillImplementations;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.DeferredRegister;
import jp.takoa.mojibattle.registry.RegistryHandle;
import jp.takoa.mojibattle.registry.SkillImplementation;

/**
 * Minecraft MODのModItems相当として、スキル実装のIDと生成Factoryだけを宣言する。
 */
public final class SkillRegistryInit {
    public static final DeferredRegister<SkillImplementation> REGISTER = DeferredRegister.create(BuiltInRegistries.SKILL_IMPLEMENTATIONS, "mojibattle");

    public static final RegistryHandle<SkillImplementation> PROJECTILE = REGISTER.register("projectile", SkillImplementations::projectile);
    public static final RegistryHandle<SkillImplementation> BIND_FOLLOW_UP = REGISTER.register("bind_follow_up", SkillImplementations::bindFollowUp);
    public static final RegistryHandle<SkillImplementation> SELF_ATTACK_BUFF = REGISTER.register("self_attack_buff", SkillImplementations::selfAttackBuff);
    public static final RegistryHandle<SkillImplementation> RANDOM_ECONOMY = REGISTER.register("random_economy", SkillImplementations::randomEconomy);

    private SkillRegistryInit() {
    }
}
