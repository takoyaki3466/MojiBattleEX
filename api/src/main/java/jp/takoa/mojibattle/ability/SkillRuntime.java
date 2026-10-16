package jp.takoa.mojibattle.ability;

/** プレイヤーごとに生成される外部スキル実装。 */
public interface SkillRuntime {
    boolean tryActivate(AbilityActivationContext context);

    default void activateFromUltimate(AbilityActivationContext context) {
        tryActivate(context);
    }
}
