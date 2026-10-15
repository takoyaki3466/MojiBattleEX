package jp.takoa.mojibattle.event;

/** イベント購読を所有者のライフサイクルに合わせて解除するハンドル。 */
@FunctionalInterface
public interface EventSubscription extends AutoCloseable {
    @Override
    void close();
}
