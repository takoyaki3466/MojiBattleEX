package jp.takoa.mojibattle.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 同期・登録順でイベントを配送する小さなイベントバス。
 *
 * <p>MODには原則として {@link RegistrationBus} 型で渡し、イベント発行権限はホスト側だけが
 * 保持する。配送中の購読変更は次回配送から反映される。</p>
 */
public final class EventBus implements RegistrationBus, AutoCloseable {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new LinkedHashMap<>();
    private boolean closed;

    @Override
    public <E> EventSubscription subscribe(Class<E> eventType, Consumer<? super E> listener) {
        if (closed) {
            throw new IllegalStateException("Event bus is closed");
        }

        if (eventType == null || listener == null) {
            throw new IllegalArgumentException("Event type and listener are required");
        }

        List<Consumer<?>> typed = listeners.computeIfAbsent(eventType, ignored -> new ArrayList<>());

        if (typed.stream().anyMatch(existing -> existing == listener)) {
            throw new IllegalArgumentException("Duplicate event listener: " + eventType.getName());
        }

        typed.add(listener);
        return new EventSubscription() {
            private boolean removed;

            @Override
            public void close() {
                if (!removed) {
                    typed.removeIf(existing -> existing == listener);
                    removed = true;
                }
            }
        };
    }

    /**
     * 完全一致するイベント型の購読者へ購読順に同期配送する。
     *
     * @param event 配送する非nullイベント
     */
    @SuppressWarnings("unchecked")
    public <E> void post(E event) {
        if (closed) {
            throw new IllegalStateException("Event bus is closed");
        }

        if (event == null) {
            throw new IllegalArgumentException("Event is required");
        }

        List<Consumer<?>> snapshot = List.copyOf(listeners.getOrDefault(event.getClass(), List.of()));

        for (Consumer<?> raw : snapshot) {
            try {
                ((Consumer<E>) raw).accept(event);
            } catch (RuntimeException error) {
                throw new EventDispatchException(event.getClass(), error);
            }
        }
    }

    @Override
    public void close() {
        listeners.clear();
        closed = true;
    }
}
