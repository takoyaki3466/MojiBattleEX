package jp.takoa.mojibattle.registry;

/**
 * Registryの不可逆な初期化状態。
 */
public enum RegistryState {
    DECLARING,
    REGISTERING,
    FROZEN,
    FAILED
}
