package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * 防御等の計算後、実際に適用されたダメージを通知する。
 */
public record DamageEvent(CharacterRuntime attacker, CharacterRuntime target, float amount) {
}
