package jp.takoa.mojibattle.event.lifecycle;

import jp.takoa.mojibattle.config.ConfigManager;

/**
 * JSONの読込、互換変換、参照検証が完了したことを通知するイベント。
 */
public record DataLoadedEvent(ConfigManager data) {
}
