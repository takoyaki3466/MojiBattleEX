package jp.takoa.mojibattle.startup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * 内部ファイルの画像をワーカースレッドで読み出してPixmapへデコードし、GLスレッドでTexture化する。
 *
 * <p>ワーカースレッドはファイルの読出しとデコード（OpenGLを使わない処理）だけを行う。
 * Texture化した結果は {@code new Texture(Gdx.files.internal(path))} と同じ画素・同じ形式になる。</p>
 */
final class ImagePreloader implements Disposable {
    private static final String THREAD_NAME = "MojiBattle-ImageDecoder";

    private final Map<String, Pending> pending = new LinkedHashMap<>();
    private ExecutorService worker;
    private boolean disposed;

    ImagePreloader(List<String> paths) {
        for (String path : paths) {
            pending.put(path, new Pending(path));
        }
    }

    void start() {
        if (worker != null || disposed) {
            return;
        }
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        try {
            for (Pending image : pending.values()) {
                worker.execute(image::run);
            }
        } catch (RejectedExecutionException rejected) {
            throw new IllegalStateException("Image decoder thread is not available", rejected);
        } finally {
            worker.shutdown();
        }
    }

    /** 全画像のデコードが終わっているか。失敗した画像があってもtrue（失敗は {@link #texture} で通知する）。 */
    boolean ready() {
        for (Pending image : pending.values()) {
            if (!image.done()) {
                return false;
            }
        }
        return true;
    }

    /**
     * デコード済みの画像をGLスレッドでTexture化する。Pixmapはここで解放する。
     *
     * @throws IllegalStateException 未登録・未完了・デコード失敗の場合
     */
    Texture texture(String path) {
        Pending image = pending.get(path);
        if (image == null) {
            throw new IllegalStateException("Image was not preloaded: " + path);
        }
        Pixmap pixmap = image.take();
        try {
            return new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
    }

    /**
     * 受け取っていないPixmapを解放し、ワーカーへ中断を伝える。複数回呼び出してもよい。
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Pending image : pending.values()) {
            image.abandon();
        }
        if (worker != null) {
            worker.shutdownNow();
        }
    }

    private static final class Pending {
        private final String path;
        private Pixmap pixmap;
        private Throwable error;
        private String errorThread;
        private boolean done;
        private boolean taken;
        private boolean abandoned;

        private Pending(String path) {
            this.path = path;
        }

        private void run() {
            synchronized (this) {
                if (abandoned) {
                    return;
                }
            }
            Pixmap decoded = null;
            Throwable failure = null;
            try {
                decoded = new Pixmap(Gdx.files.internal(path));
            } catch (Throwable thrown) {
                failure = thrown;
            }
            synchronized (this) {
                if (abandoned) {
                    if (decoded != null) {
                        decoded.dispose();
                    }
                    return;
                }
                pixmap = decoded;
                error = failure;
                errorThread = Thread.currentThread().getName();
                done = true;
            }
            if (failure instanceof VirtualMachineError fatal) {
                throw fatal;
            }
        }

        private synchronized boolean done() {
            return done;
        }

        private synchronized Pixmap take() {
            if (!done || taken) {
                throw new IllegalStateException("Image is not available: " + path);
            }
            if (error != null) {
                throw new IllegalStateException("Cannot decode image '" + path + "' on thread " + errorThread, error);
            }
            taken = true;
            Pixmap value = pixmap;
            pixmap = null;
            return value;
        }

        private synchronized void abandon() {
            abandoned = true;
            if (pixmap != null) {
                pixmap.dispose();
                pixmap = null;
            }
        }
    }
}
