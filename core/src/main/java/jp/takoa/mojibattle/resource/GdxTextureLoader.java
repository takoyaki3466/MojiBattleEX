package jp.takoa.mojibattle.resource;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.io.IOException;
import java.io.InputStream;

/**
 * 公開ResourceManagerのstreamをlibGDX Textureへ変換する描画境界アダプター。
 */
public final class GdxTextureLoader {
    private final ResourceManager resources;

    public GdxTextureLoader(ResourceManager resources) {
        this.resources = resources;
    }

    /**
     * リソースIDまたは従来パスからTextureを生成する。返却Textureは呼出し側が破棄する。
     */
    public Texture load(String reference, boolean mipmaps) {
        ResourceId id = ResourceId.parse(reference);
        try (InputStream stream = resources.open(ResourceKind.ASSET, id)) {
            byte[] encoded = stream.readAllBytes();

            Pixmap pixmap = new Pixmap(encoded, 0, encoded.length);

            try {
                return new Texture(pixmap, mipmaps);

            } finally {
                pixmap.dispose();
            }

        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load texture resource: " + id, error);
        }
    }
}
