package jp.takoa.mojibattle.ability;

/** 補助キャラクター個体ごとに生成されるスキルRuntime。 */
@FunctionalInterface
public interface SupportSkillRuntime {
    boolean activate(SupportAbilityActivationContext context);
}
