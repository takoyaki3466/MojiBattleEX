package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/** 多段数や弾数にかかわらず、通常攻撃の動作1回につき1回通知される。命中イベントとは独立する。 */
public record NormalAttackEvent(String attackerInstanceId, ResourceId characterId, Team team, AttackerType attackerType) {
    public NormalAttackEvent {
        Objects.requireNonNull(attackerInstanceId, "attackerInstanceId");
        Objects.requireNonNull(characterId, "characterId");
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(attackerType, "attackerType");
    }
    public enum Team { ALLY, ENEMY }
    public enum AttackerType { MAIN, SUPPORT }
    public AttackType attackType() { return AttackType.NORMAL; }
    public enum AttackType { NORMAL }
}
