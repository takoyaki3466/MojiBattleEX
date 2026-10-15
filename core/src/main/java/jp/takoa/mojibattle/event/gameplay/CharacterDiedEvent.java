package jp.takoa.mojibattle.event.gameplay;

import jp.takoa.mojibattle.domain.CharacterRuntime;

/**
 * 生存状態から死亡状態へ遷移したキャラクターを通知する。
 */
public record CharacterDiedEvent(CharacterRuntime character) {
}
