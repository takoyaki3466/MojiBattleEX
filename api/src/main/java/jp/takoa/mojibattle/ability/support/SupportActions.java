package jp.takoa.mojibattle.ability.support;

import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.battle.modifier.SupportNormalAttackModifier;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * 呼出元の補助個体だけを変更できる、補助専用の操作境界。
 *
 * <h2>関係</h2>
 * <p>通常攻撃変更には{@link SupportNormalAttackModifier}、HUD表示には{@link AbilityStateLine}、
 * 一時分裂には{@link SupportSplitSpec}を使用する。
 * {@link jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext}と
 * {@link jp.takoa.mojibattle.ability.context.SupportPassiveContext}から提供され、ホストの補助操作アダプターが実装する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * context.supportActions().addNormalAttackModifier(modifier, 5f);
 * context.supportActions().setStateLines(List.of(
 *         AbilityStateLine.of("mod.support.boost", 20, 5)));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>{@link jp.takoa.mojibattle.battle.action.CombatActions}の全体操作と、補助個体だけに許された変更を権限境界として分離するために作成された。</p>
 */
public interface SupportActions {
    void addNormalAttackModifier(SupportNormalAttackModifier modifier);
    void addNormalAttackModifier(SupportNormalAttackModifier modifier, float durationSeconds);
    List<SupportNormalAttackModifier> normalAttackModifiers();
    boolean removeNormalAttackModifier(ResourceId id);
    void setStateLines(List<AbilityStateLine> lines);

    /** Returns points to this support without applying gain multipliers. */
    default void refundUltimatePoints(float points) {
        throw new UnsupportedOperationException("Support ultimate refunds are not supported by this host");
    }

    /** 補助を指定された一時召喚個体へ分裂させる。 */
    default void splitTemporarily(SupportSplitSpec spec) {
        throw new UnsupportedOperationException("Temporary support splitting is not supported by this host");
    }

    /** 生存中のメイン・補助・召喚数を使う動的な攻撃倍率を登録する。 */
    default void setBattlefieldCountAttackMultiplier(ResourceId id, int priority,
            float baseMultiplier, float multiplierPerCombatant) {
        throw new UnsupportedOperationException("Battlefield-count modifiers are not supported by this host");
    }
}
