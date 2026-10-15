package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * 攻撃が生成された時点の通知。ダメージ適用通知とは区別される。
 */
public record AttackEvent(CharacterRuntime attacker, String abilityId, boolean ultimate) {
}
