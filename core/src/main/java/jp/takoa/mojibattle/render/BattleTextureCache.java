package jp.takoa.mojibattle.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import jp.takoa.mojibattle.resource.GdxTextureLoader;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 戦闘描画で使用するTextureを参照文字列ごとに一度だけ生成して保持するキャッシュ。
 *
 * <p>同じ画像を複数の描画モデルで共有し、重複ロードを防ぐ。保持するTextureの所有権はこのキャッシュにあり、
 * {@link #dispose()} で一括解放する。GLスレッドからのみ使用すること。</p>
 */
public final class BattleTextureCache implements Disposable {
    private final GdxTextureLoader loader;
    private final Map<String, Texture> textures = new LinkedHashMap<>();
    private boolean disposed;

    public BattleTextureCache(ResourceManager resources) {
        loader = new GdxTextureLoader(resources);
    }

    /**
     * キャッシュ済みTextureを返し、未登録なら同期的に読み込んで登録する。
     */
    public Texture get(String reference) {
        ensureOpen();
        Texture cached = textures.get(reference);
        if (cached != null) {
            return cached;
        }
        Texture loaded = loader.load(reference, true);
        return register(reference, loaded);
    }

    /**
     * ワーカースレッドでデコード済みのPixmapをGPUへ転送して登録する。Pixmapの所有権は呼出し側に残る。
     *
     * @return 登録済みTexture。既に同じ参照が存在する場合はそのTexture
     */
    public Texture upload(String reference, Pixmap pixmap) {
        ensureOpen();
        Texture cached = textures.get(reference);
        if (cached != null) {
            return cached;
        }
        Texture uploaded;
        try {
            uploaded = GdxTextureLoader.upload(pixmap, true);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Cannot upload texture resource: " + reference, error);
        }
        return register(reference, uploaded);
    }

    public boolean contains(String reference) {
        return textures.containsKey(reference);
    }

    public int size() {
        return textures.size();
    }

    private Texture register(String reference, Texture texture) {
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        textures.put(reference, texture);
        return texture;
    }

    private void ensureOpen() {
        if (disposed) {
            throw new IllegalStateException("Battle texture cache is already disposed");
        }
    }

    /**
     * 保持する全Textureを解放する。複数回呼び出しても二重解放しない。
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        RuntimeException failure = null;
        for (Texture texture : textures.values()) {
            try {
                texture.dispose();
            } catch (RuntimeException error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        textures.clear();
        if (failure != null) {
            throw failure;
        }
    }
}
