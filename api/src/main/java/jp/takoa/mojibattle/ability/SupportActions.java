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
}
