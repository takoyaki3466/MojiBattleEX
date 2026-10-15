package jp.takoa.mojibattle.event;

/**
 * 起動時だけ使うライフサイクルイベントと、戦闘中のゲームイベントを別バスへ隔離する。
 * GameContextが所有し、終了時に両方を閉じる。
 */
public final class EventChannels implements AutoCloseable {
    private final EventBus lifecycle = new EventBus();
    private final EventBus gameplay = new EventBus();

    public EventBus lifecycle() {
        return lifecycle;
    }

    public EventBus gameplay() {
        return gameplay;
    }

    @Override
    public void close() {
        gameplay.close();
        lifecycle.close();
    }
}
