package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.NormalAttackEvent;

import java.util.List;

/** 補助キャラクターの戦闘個体ごとに生成されるパッシブ。 */
public interface SupportPassiveRuntime {
    default void onBattleStart(SupportPassiveContext context) { }
    default void update(SupportPassiveContext context, float deltaSeconds) { }
    default void onSkillActivated(SupportPassiveContext context) { }
    default void onUltimateActivated(SupportPassiveContext context) { }
    default void onNormalAttack(SupportPassiveContext context, NormalAttackEvent event) { }
    default List<AbilityStateLine> stateLines(SupportPassiveContext context) { return List.of(); }
    default void onBattleEnd(SupportPassiveContext context) { }
}
