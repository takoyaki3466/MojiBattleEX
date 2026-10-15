package jp.takoa.mojibattle.event.lifecycle;

import jp.takoa.mojibattle.registry.RegistryManager;

/**
 * 全レジストリが凍結され、以後内容を変更できないことを通知するイベント。
 */
public record RegistriesFrozenEvent(RegistryManager registries) {
}
