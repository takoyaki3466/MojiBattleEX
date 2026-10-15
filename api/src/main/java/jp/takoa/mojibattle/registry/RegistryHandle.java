package jp.takoa.mojibattle.registry;

import java.util.function.Supplier;

/** 登録確定後だけ値を返す遅延参照。 */
public final class RegistryHandle<T> implements Supplier<T> {
    private final RegistryId id;
    private T value;

    RegistryHandle(RegistryId id) {
        this.id = id;
    }

    public RegistryId id() {
        return id;
    }

    public boolean isBound() {
        return value != null;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Registry handle was accessed before registration: " + id);
        }
        return value;
    }

    void bind(T registeredValue) {
        if (registeredValue == null) {
            throw new IllegalArgumentException("Cannot bind null to " + id);
        }
        if (value != null) {
            throw new IllegalStateException("Registry handle is already bound: " + id);
        }
        value = registeredValue;
    }
}
