package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.event.EventSubscription;
import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.event.lifecycle.RegisterEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 登録宣言と実体生成を分離し、登録イベントで一括確定する。
 */
public final class CompatRegister<T> {
    private final RegistryKey<T> registryKey;
    private final String namespace;
    private final Map<RegistryHandle<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private EventSubscription subscription;
    private boolean resolved;

    private CompatRegister(RegistryKey<T> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = RegistryId.of(namespace, "validation").namespace();
    }

    public static <T> CompatRegister<T> create(RegistryKey<T> registryKey, String namespace) {
        return new CompatRegister<>(registryKey, namespace);
    }

    public RegistryHandle<T> register(String path, Supplier<? extends T> factory) {
        if (subscription != null || resolved) {
            throw new IllegalStateException("Cannot declare entries after DeferredRegister attachment");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Registry factory is required");
        }
        RegistryHandle<T> handle = new RegistryHandle<>(RegistryId.of(namespace, path));
        if (entries.keySet().stream().anyMatch(existing -> existing.id().equals(handle.id()))) {
            throw new IllegalArgumentException("Duplicate deferred registration: " + handle.id());
        }
        entries.put(handle, factory);
        return handle;
    }

    /**
     * 登録イベントを受け取るため、購読専用バスへ一度だけ接続する。
     */
    public void attach(RegistrationBus registrationBus) {
        if (subscription != null) {
            throw new IllegalStateException("DeferredRegister is already attached: " + registryKey.id());
        }
        subscription = registrationBus.subscribe(RegisterEvent.class, this::onRegister);
    }

    public RegistryKey<T> registryKey() {
        return registryKey;
    }

    public java.util.Collection<RegistryHandle<T>> entries() {
        return List.copyOf(entries.keySet());
    }

    /**
     * 全Factoryを先に評価・検証し、成功した場合だけ登録とHandle束縛を行う。
     */
    @SuppressWarnings("unchecked")
    private void onRegister(RegisterEvent<?> rawEvent) {
        if (!registryKey.equals(rawEvent.key())) {
            return;
        }
        if (resolved) {
            throw new IllegalStateException("DeferredRegister was resolved twice: " + registryKey.id());
        }
        Registry<T> registry = (Registry<T>) rawEvent.registry();
        List<Map.Entry<RegistryHandle<T>, T>> staged = new ArrayList<>();
        for (Map.Entry<RegistryHandle<T>, Supplier<? extends T>> entry : entries.entrySet()) {
            T value = entry.getValue().get();
            registry.validate(entry.getKey().id(), value);
            if (staged.stream().anyMatch(valueEntry -> valueEntry.getKey().id().equals(entry.getKey().id()))) {
                throw new IllegalArgumentException("Duplicate staged registration: " + entry.getKey().id());
            }
            staged.add(Map.entry(entry.getKey(), value));
        }
        for (Map.Entry<RegistryHandle<T>, T> entry : staged) {
            registry.register(entry.getKey().id(), entry.getValue());
        }
        for (Map.Entry<RegistryHandle<T>, T> entry : staged) {
            entry.getKey().bind(entry.getValue());
        }
        resolved = true;
        subscription.close();
    }
}
