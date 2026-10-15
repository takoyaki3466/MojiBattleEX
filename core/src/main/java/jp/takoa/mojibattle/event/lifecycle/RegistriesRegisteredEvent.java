package jp.takoa.mojibattle.event.lifecycle;

import jp.takoa.mojibattle.registry.RegistryManager;

/**
 * RegisterEventの配送が完了し、JSONから実装IDを解決できる段階になったことを通知する。
 */
public record RegistriesRegisteredEvent(RegistryManager registries) {
}
