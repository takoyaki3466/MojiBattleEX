package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * 戦闘用キャラクターのプレイヤー固有インスタンスが生成された直後のイベント。
 */
public record CharacterSpawnedEvent(CharacterRuntime character) {
}
