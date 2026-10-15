package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * スキルの発動可否判定に成功した後のイベント。
 */
public record SkillActivatedEvent(CharacterRuntime character, String skillId) {
}
