package jp.takoa.mojibattle.resource;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.io.IOException;
import java.io.InputStream;

/**
 * 公開ResourceManagerのstreamをlibGDX Textureへ変換する描画境界アダプター。
 *
 * <p>読込みは「streamの読出しと画像デコード（{@link #decode(String)}）」と
 * 「GPUへの転送（{@link #upload(Pixmap, boolean)}）」の二段階に分かれる。
 * 前者はOpenGLを使用しないためワーカースレッドで実行でき、後者は必ずGLスレッドで実行する。</p>
 */
public final class GdxTextureLoader {
    private final ResourceManager resources;

    public GdxTextureLoader(ResourceManager resources) {
        this.resources = resources;
    }

    /**
     * リソースIDまたは従来パスからTextureを生成する。返却Textureは呼出し側が破棄する。
     * GLスレッドから呼び出すこと。
     */
    public Texture load(String reference, boolean mipmaps) {
        Pixmap pixmap = decode(reference);
        try {
            return upload(pixmap, mipmaps);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Cannot load texture resource: " + reference, error);
        } finally {
            pixmap.dispose();
        }
    }

    /**
     * リソースを読み出してPixmapへデコードする。OpenGLを使用しないため任意スレッドから呼び出せる。
     * 返却Pixmapは呼出し側が破棄する。
     */
    public Pixmap decode(String reference) {
        ResourceId id = ResourceId.parse(reference);
        try (InputStream stream = resources.open(ResourceKind.ASSET, id)) {
            byte[] encoded = stream.readAllBytes();
            return new Pixmap(encoded, 0, encoded.length);

        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load texture resource: " + id, error);
        }
    }

    /**
     * デコード済みPixmapからTextureを生成する。Pixmapの所有権は呼出し側に残る。
     * GLスレッドから呼び出すこと。
     */
    public static Texture upload(Pixmap pixmap, boolean mipmaps) {
        return new Texture(pixmap, mipmaps);
    }
}
