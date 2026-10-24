package jp.takoa.mojibattle.ability.runtime;

import jp.takoa.mojibattle.ability.context.SupportPassiveContext;
import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.battle.event.NormalAttackEvent;

import java.util.List;

/**
 * 補助キャラクターの戦闘個体ごとに生成される、ライフサイクル型パッシブ契約。
 *
 * <h2>関係</h2>
 * <p>全コールバックは{@link SupportPassiveContext}を受け取り、通常攻撃時だけ
 * {@link NormalAttackEvent}も受け取る。表示値は{@link AbilityStateLine}として返す。
 * 補助パッシブ登録の{@link AbilityFactory}が生成し、ホストの補助コントローラーから依存される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SupportPassiveRuntime passive = new SupportPassiveRuntime() {
 *     public void onBattleStart(SupportPassiveContext context) {
 *         context.state().putInt("attacks", 0);
 *     }
 *     public void onNormalAttack(SupportPassiveContext context, NormalAttackEvent event) {
 *         context.state().putInt("attacks", context.state().getInt("attacks", 0) + 1);
 *     }
 * };
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>補助はメインパッシブと異なる開始・終了・通常攻撃通知を必要とするため、専用ライフサイクルとして分離された。</p>
 */
public interface SupportPassiveRuntime {

    default void onBattleStart(SupportPassiveContext context) {
    }

    default void update(SupportPassiveContext context, float deltaSeconds) {
    }

    default void onSkillActivated(SupportPassiveContext context) {
    }

    default void onUltimateActivated(SupportPassiveContext context) {
    }

    default void onNormalAttack(SupportPassiveContext context, NormalAttackEvent event) {
    }

    default List<AbilityStateLine> stateLines(SupportPassiveContext context) {
        return List.of();
    }

    default void onBattleEnd(SupportPassiveContext context) {
    }
}
