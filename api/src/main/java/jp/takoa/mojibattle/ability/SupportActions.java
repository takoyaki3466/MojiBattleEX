package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.SupportNormalAttackModifier;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 呼出元の補助個体だけを変更できる操作境界。 */
public interface SupportActions {
    void addNormalAttackModifier(SupportNormalAttackModifier modifier);
    void addNormalAttackModifier(SupportNormalAttackModifier modifier, float durationSeconds);
    List<SupportNormalAttackModifier> normalAttackModifiers();
    boolean removeNormalAttackModifier(ResourceId id);
    void setStateLines(List<AbilityStateLine> lines);

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
