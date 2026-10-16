package jp.takoa.mojibattle.ability;

/** 補助キャラクター個体ごとに生成される必殺Runtime。 */
@FunctionalInterface
public interface SupportUltimateRuntime {
    boolean activate(SupportAbilityActivationContext context);
}
