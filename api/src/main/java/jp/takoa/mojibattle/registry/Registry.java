package jp.takoa.mojibattle.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ID、型、登録順と凍結状態を管理するRegistry。
 */
public final class Registry<T> {
    private final String kind;
    private final Function<T, String> idExtractor;
    private final BiConsumer<RegistryId, T> registrationValidator;
    private final Map<RegistryId, T> values = new LinkedHashMap<>();
    private RegistryState state = RegistryState.DECLARING;

    public Registry(String kind, Function<T, String> idExtractor) {
        if (kind == null || kind.isBlank() || idExtractor == null) {
            throw new IllegalArgumentException("Registry metadata is required");
        }
        this.kind = kind;
        this.idExtractor = idExtractor;
        this.registrationValidator = (registrationId, value) -> {
            RegistryId valueId = RegistryId.parse(idExtractor.apply(value));
            if (!registrationId.equals(valueId)) {
                throw new IllegalArgumentException("Invalid registration: registry=" + kind
                        + ", valueType=" + value.getClass().getName()
                        + ", registrationId=" + registrationId
                        + ", valueId=" + valueId
                        + ", rule=Value ID must match registration ID");
            }
        };
    }

    /** 値から推定する既定IDと、明示IDの型固有検証を別々に指定する。 */
    Registry(String kind, Function<T, String> idExtractor,
             BiConsumer<RegistryId, T> registrationValidator) {
        if (kind == null || kind.isBlank() || idExtractor == null || registrationValidator == null) {
            throw new IllegalArgumentException("Registry metadata is required");
        }
        this.kind = kind;
        this.idExtractor = idExtractor;
        this.registrationValidator = registrationValidator;
    }

    public T register(T value) {
        if (value == null) {
            throw new IllegalArgumentException(kind + " must not be null");
        }
        return register(RegistryId.parse(idExtractor.apply(value)), value);
    }

    public T register(RegistryId id, T value) {
        ensureMutable();
        validate(id, value);
        if (values.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Duplicate " + kind + " ID: " + id);
        }
        return value;
    }

    /**
     * 登録前検証用。Registryの内容は変更しない。
     */
    void validate(RegistryId id, T value) {
        if (id == null || value == null) {
            throw new IllegalArgumentException(kind + " ID and value are required");
        }
        registrationValidator.accept(id, value);
        if (values.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate " + kind + " ID: " + id);
        }
    }

    public T require(String id) {
        T value = get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + kind + " ID: " + id);
        }
        return value;
    }

    public T get(String id) {
        return id == null ? null : values.get(RegistryId.parse(id));
    }

    public void replace(String id, T value) {
        ensureMutable();
        RegistryId key = RegistryId.parse(id);
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("Cannot replace unknown " + kind + " ID: " + id);
        }
        if (value == null) {
            throw new IllegalArgumentException(kind + " must not be null");
        }
        registrationValidator.accept(key, value);
        values.put(key, value);
    }

    public void removeIf(Predicate<T> predicate) {
        ensureMutable();
        values.values().removeIf(predicate);
    }

    public boolean contains(String id) {
        return id != null && values.containsKey(RegistryId.parse(id));
    }

    public Collection<T> values() {
        return List.copyOf(values.values());
    }

    public void freeze() {
        ensureNotFailed();
        state = RegistryState.FROZEN;
    }

    public boolean frozen() {
        return state == RegistryState.FROZEN;
    }

    void beginRegistration() {
        if (state != RegistryState.DECLARING) {
            throw new IllegalStateException(kind + " registry cannot begin registration from " + state);
        }
        state = RegistryState.REGISTERING;
    }

    void fail() {
        state = RegistryState.FAILED;
    }

    public RegistryState state() {
        return state;
    }

    private void ensureMutable() {
        if (state == RegistryState.FROZEN || state == RegistryState.FAILED) {
            throw new IllegalStateException(kind + " registry is " + state);
        }
    }

    private void ensureNotFailed() {
        if (state == RegistryState.FAILED) {
            throw new IllegalStateException(kind + " registry is failed");
        }
    }
}
