package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.event.lifecycle.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Registryの生成、登録イベント配送、失敗状態と一括凍結を管理する。
 */
public final class RegistryManager {
    private final Map<RegistryKey<?>, Registry<?>> registries = new LinkedHashMap<>();

    private boolean registrationStarted;
    private boolean frozen;
    private boolean failed;

    public <T> Registry<T> create(RegistryKey<T> key, String kind, Function<T, String> idExtractor) {
        return add(key, new Registry<>(kind, idExtractor));
    }

    /**
     * 値から推定する既定IDと、明示IDの型固有検証を分離したRegistryを作成する。
     */
    public <T> Registry<T> create(RegistryKey<T> key, String kind, Function<T, String> idExtractor, BiConsumer<RegistryId, T> registrationValidator) {
        return add(key, new Registry<>(kind, idExtractor, registrationValidator));
    }

    private <T> Registry<T> add(RegistryKey<T> key, Registry<T> registry) {
        if (registrationStarted || frozen || failed) {
            throw new IllegalStateException("Cannot create registries after registration started");
        }
        if (registries.keySet().stream().anyMatch(existing -> existing.id().equals(key.id()))) {
            throw new IllegalArgumentException("Duplicate registry ID: " + key.id());
        }
        registries.put(key, registry);
        return registry;
    }

    /**
     * Registry作成順に登録イベントを発行し、失敗時は全RegistryをFAILEDへ遷移させる。
     */
    public void publishRegistrationEvents(EventBus lifecycleBus) {
        if (registrationStarted) {
            throw new IllegalStateException("Registry events were already published");
        }

        registrationStarted = true;
        registries.values().forEach(Registry::beginRegistration);

        try {
            registries.forEach((key, value) -> post(lifecycleBus, key, value));

        } catch (RuntimeException error) {
            failed = true;
            registries.values().forEach(Registry::fail);
            throw error;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void post(EventBus bus, RegistryKey<?> key, Registry<?> registry) {
        bus.post(new RegisterEvent((RegistryKey) key, (Registry) registry));
    }

    @SuppressWarnings("unchecked")
    public <T> Registry<T> require(RegistryKey<T> key) {
        Registry<?> registry = registries.get(key);

        if (registry == null) {
            throw new IllegalArgumentException("Unknown registry: " + key.id());
        }
        return (Registry<T>) registry;
    }

    public void freezeAll() {
        if (frozen || failed) {
            throw new IllegalStateException("Registries cannot be frozen from current state");
        }
        if (!registrationStarted) {
            registrationStarted = true;
            registries.values().forEach(Registry::beginRegistration);
        }
        registries.values().forEach(Registry::freeze);
        frozen = true;
    }

    public boolean frozen() {
        return frozen;
    }

    public boolean failed() {
        return failed;
    }
}
