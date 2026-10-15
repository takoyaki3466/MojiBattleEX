package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

/** Ability実装がcore内部型へ触れずに要求できる戦闘操作の最小境界。 */
public interface CombatActions {
    void damage(CombatantView source, CombatantView target, float amount);

    void heal(CombatantView target, float amount);

    void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds);
}
