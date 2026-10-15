package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * 必殺技のコスト消費と発動判定に成功した後のイベント。
 */
public record UltimateActivatedEvent(CharacterRuntime character, String ultimateId) {
}
