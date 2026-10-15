package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.bootstrap.GameBootstrap;
import jp.takoa.mojibattle.registry.RegistrySnapshot;

/**
 * 旧公開APIを維持する移行用ファサード。
 * 新規コードは {@link GameBootstrap} を使用する。
 */
@Deprecated
public final class ItemRegistryBootstrap {
    private ItemRegistryBootstrap() {
    }

    public static RegistrySnapshot bootstrap() {
        return GameBootstrap.bootstrap();
    }
}
