package jp.takoa.mojibattle.event.lifecycle;

import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.RegistryKey;

/** 特定の型付きRegistryが登録を受け付ける段階になったことを通知する。 */
public record RegisterEvent<T>(RegistryKey<T> key, Registry<T> registry) {
}
