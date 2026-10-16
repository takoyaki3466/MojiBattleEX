package jp.takoa.mojibattle.ability;

/** プレイヤーごとに生成される外部必殺技実装。 */
@FunctionalInterface
public interface UltimateRuntime {
    boolean tryActivate(AbilityActivationContext context);
}
