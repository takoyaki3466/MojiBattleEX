package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * FreeTypeによるグリフのラスタライズをワーカースレッドで行い、GPUへの転送だけをGLスレッドで行うフォント生成器。
 *
 * <p>ワーカースレッドでは {@link FreeTypeFontGenerator#generateData(FreeTypeFontGenerator.FreeTypeFontParameter)} に
 * 外部の {@link PixmapPacker} を渡し、OpenGLを使わずにPixmapへ描き込むところまでを行う。
 * GLスレッドは {@link #poll()} で完成した分を1件ずつTexture化して {@link BitmapFont} を作る。
 * 結果は {@link FreeTypeFontGenerator#generateFont(FreeTypeFontGenerator.FreeTypeFontParameter)} と同じグリフ・同じ設定のフォントになる。</p>
 *
 * <p>FreeTypeの生成器はワーカースレッドで生成・使用・解放し、他スレッドと共有しない。</p>
 */
public final class AsyncFontGenerator implements Disposable {
    private static final String THREAD_NAME = "MojiBattle-FontRasterizer";

    private final FileHandle fontFile;
    private final List<UiFactory.FontSpec> specs;
    private final List<Result> results = new ArrayList<>();
    private ExecutorService worker;
    private int taken;
    private boolean disposed;

    /**
     * @param fontFile フォントファイル
     * @param specs    生成するフォント。パラメータの {@code packer} はこのクラスが設定する
     */
    public AsyncFontGenerator(FileHandle fontFile, List<UiFactory.FontSpec> specs) {
        this.fontFile = fontFile;
        this.specs = List.copyOf(specs);
        for (UiFactory.FontSpec spec : this.specs) {
            if (spec.parameter().packer != null || spec.parameter().incremental) {
                throw new IllegalArgumentException("Font spec must not use a custom packer or incremental glyphs: " + spec.name());
            }
            results.add(new Result(spec.name()));
        }
    }

    /**
     * ワーカースレッドでラスタライズを開始する。二度目以降の呼出しは何もしない。
     */
    public void start() {
        if (worker != null || disposed) {
            return;
        }
        worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        try {
            worker.execute(this::rasterizeAll);
        } catch (RejectedExecutionException rejected) {
            throw new IllegalStateException("Font rasterizer thread is not available", rejected);
        } finally {
            // 投入済みのタスクだけを実行させ、完了後にスレッドを終了させる。
            worker.shutdown();
        }
    }

    /** 生成するフォントの数。 */
    public int total() {
        return specs.size();
    }

    /** {@link #poll()} で受け取り済みのフォントの数。 */
    public int completed() {
        return taken;
    }

    /** 次に受け取るフォントの名前。全て受け取った後はnull。 */
    public String nextName() {
        return taken < specs.size() ? specs.get(taken).name() : null;
    }

    /**
     * 次のフォントのラスタライズが終わっていれば、GLスレッドでTexture化して返す。
     *
     * @return 完成したフォント。未完了の場合、または全て受け取り済みの場合はnull
     * @throws IllegalStateException ワーカースレッドでの生成に失敗した場合
     */
    public NamedFont poll() {
        if (disposed) {
            throw new IllegalStateException("Font generator is disposed");
        }
        if (taken >= specs.size()) {
            return null;
        }
        Result result = results.get(taken);
        Rasterized rasterized = result.take();
        if (rasterized == null) {
            return null;
        }
        UiFactory.FontSpec spec = specs.get(taken);
        BitmapFont font;
        try {
            font = upload(spec.parameter(), rasterized);
        } catch (RuntimeException | Error error) {
            rasterized.packer.dispose();
            throw error;
        }
        taken++;
        return new NamedFont(spec.name(), font);
    }

    /**
     * まだ受け取っていない生成結果を解放し、ワーカースレッドへ中断を伝える。複数回呼び出してもよい。
     * 受け取り済みのフォントは呼出し側が解放する。
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Result result : results) {
            result.abandon();
        }
        if (worker != null) {
            worker.shutdownNow();
        }
    }

    private void rasterizeAll() {
        FreeTypeFontGenerator generator = null;
        int index = 0;
        try {
            generator = new FreeTypeFontGenerator(fontFile);
            for (; index < specs.size(); index++) {
                Result result = results.get(index);
                if (result.abandoned()) {
                    return;
                }
                result.complete(rasterize(generator, specs.get(index).parameter()));
            }
        } catch (Throwable error) {
            if (index < results.size()) {
                results.get(index).fail(error, Thread.currentThread().getName());
            }
            for (int rest = index + 1; rest < results.size(); rest++) {
                results.get(rest).fail(new IllegalStateException("Skipped because font '" + specs.get(index).name() + "' failed"), Thread.currentThread().getName());
            }
            if (error instanceof VirtualMachineError fatal) {
                throw fatal;
            }
        } finally {
            if (generator != null) {
                generator.dispose();
            }
        }
    }

    private static Rasterized rasterize(FreeTypeFontGenerator generator, FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        PixmapPacker packer = createPacker(parameter);
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter copy = copyWithPacker(parameter, packer);
            FreeTypeFontGenerator.FreeTypeBitmapFontData data = generator.generateData(copy);
            return new Rasterized(data, packer);
        } catch (RuntimeException | Error error) {
            packer.dispose();
            throw error;
        }
    }

    /**
     * {@link FreeTypeFontGenerator} が自前で作るPackerと同じ形式・同じ透過色のPackerを作る。
     * ページサイズは同クラスの上限値を使う（Skinのように収録文字が多い場合、同クラスの計算結果も上限値になる）。
     */
    static PixmapPacker createPacker(FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        int size = FreeTypeFontGenerator.getMaxTextureSize();
        PixmapPacker packer = new PixmapPacker(size, size, Pixmap.Format.RGBA8888, 1, false, new PixmapPacker.SkylineStrategy());
        packer.setTransparentColor(parameter.color);
        packer.getTransparentColor().a = 0;
        if (parameter.borderWidth > 0) {
            packer.setTransparentColor(parameter.borderColor);
            packer.getTransparentColor().a = 0;
        }
        return packer;
    }

    private static FreeTypeFontGenerator.FreeTypeFontParameter copyWithPacker(FreeTypeFontGenerator.FreeTypeFontParameter source, PixmapPacker packer) {
        FreeTypeFontGenerator.FreeTypeFontParameter copy = new FreeTypeFontGenerator.FreeTypeFontParameter();
        copy.size = source.size;
        copy.mono = source.mono;
        copy.hinting = source.hinting;
        copy.color = source.color;
        copy.gamma = source.gamma;
        copy.renderCount = source.renderCount;
        copy.borderWidth = source.borderWidth;
        copy.borderColor = source.borderColor;
        copy.borderStraight = source.borderStraight;
        copy.borderGamma = source.borderGamma;
        copy.shadowOffsetX = source.shadowOffsetX;
        copy.shadowOffsetY = source.shadowOffsetY;
        copy.shadowColor = source.shadowColor;
        copy.spaceX = source.spaceX;
        copy.spaceY = source.spaceY;
        copy.padTop = source.padTop;
        copy.padLeft = source.padLeft;
        copy.padBottom = source.padBottom;
        copy.padRight = source.padRight;
        copy.characters = source.characters;
        copy.kerning = source.kerning;
        copy.flip = source.flip;
        copy.genMipMaps = source.genMipMaps;
        copy.minFilter = source.minFilter;
        copy.magFilter = source.magFilter;
        copy.incremental = false;
        copy.packer = packer;
        return copy;
    }

    /**
     * {@link FreeTypeFontGenerator#generateFont(FreeTypeFontGenerator.FreeTypeFontParameter)} の後半（Texture化）と同じ処理。
     * このPackerはこのフォント専用のため、TextureはフォントがBitmapFont#dispose()で解放する。
     */
    private static BitmapFont upload(FreeTypeFontGenerator.FreeTypeFontParameter parameter, Rasterized rasterized) {
        Array<TextureRegion> regions = new Array<>();
        rasterized.packer.updateTextureRegions(regions, parameter.minFilter, parameter.magFilter, parameter.genMipMaps);
        if (regions.isEmpty()) {
            throw new IllegalStateException("Unable to create a font with no texture regions.");
        }
        rasterized.data.regions = regions;
        BitmapFont font = new BitmapFont(rasterized.data, regions, true);
        font.setOwnsTexture(true);
        return font;
    }

    /** 生成したフォントとSkinでの登録名。 */
    public record NamedFont(String name, BitmapFont font) {
    }

    private record Rasterized(FreeTypeFontGenerator.FreeTypeBitmapFontData data, PixmapPacker packer) {
    }

    /**
     * ワーカースレッドからGLスレッドへ1件の結果を受け渡す。受け渡し前に破棄された場合はPixmapを解放する。
     */
    private static final class Result {
        private final String name;
        private Rasterized rasterized;
        private Throwable error;
        private String errorThread;
        private boolean done;
        private boolean abandoned;

        private Result(String name) {
            this.name = name;
        }

        private synchronized boolean abandoned() {
            return abandoned;
        }

        private void complete(Rasterized value) {
            synchronized (this) {
                if (!abandoned) {
                    rasterized = value;
                    done = true;
                    return;
                }
            }
            value.packer.dispose();
        }

        private synchronized void fail(Throwable failure, String thread) {
            error = failure;
            errorThread = thread;
            done = true;
        }

        private synchronized Rasterized take() {
            if (!done) {
                return null;
            }
            if (error != null) {
                if (error instanceof VirtualMachineError fatal) {
                    throw fatal;
                }
                throw new IllegalStateException("Cannot rasterize font '" + name + "' on thread " + errorThread, error);
            }
            Rasterized value = rasterized;
            rasterized = null;
            return value;
        }

        private synchronized void abandon() {
            abandoned = true;
            if (rasterized != null) {
                rasterized.packer.dispose();
                rasterized = null;
            }
        }
    }
}
