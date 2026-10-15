package jp.takoa.mojibattle.event;

/** リスナー例外へ配送中のイベント型を付加する実行時例外。 */
public final class EventDispatchException extends RuntimeException {
    public EventDispatchException(Class<?> eventType, RuntimeException cause) {
        super("Event listener failed for " + eventType.getName(), cause);
    }
}
