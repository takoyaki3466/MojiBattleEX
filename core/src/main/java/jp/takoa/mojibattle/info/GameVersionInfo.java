package jp.takoa.mojibattle.info;

import jp.takoa.mojibattle.mod.GameApiVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 情報画面とクラッシュレポートに表示するバージョン情報。
 *
 * <p>ゲームバージョン（例: {@code v1.1-release-0}）はビルド時にルートの {@code build.gradle.kts} の
 * {@code version} から生成したリソース {@value #RESOURCE} を読む。APIバージョンは {@link GameApiVersion} の値を使う。</p>
 */
public final class GameVersionInfo {
    static final String RESOURCE = "META-INF/mojibattle/game-version.properties";
    private static final String UNKNOWN = "unknown";

    private static volatile String cachedGameVersion;

    private GameVersionInfo() {
    }

    /**
     * ゲームのバージョン（例: {@code v1.1-release-0}）。リソースが無い場合は {@code unknown}。
     */
    public static String gameVersion() {
        String version = cachedGameVersion;
        if (version == null) {
            version = read(GameVersionInfo.class.getClassLoader());
            cachedGameVersion = version;
        }
        return version;
    }

    /**
     * {@code メジャー.マイナー.パッチ} 形式のAPIバージョン（例: {@code 1.1.0}）。
     */
    public static String apiVersion() {
        return GameApiVersion.API_VERSION;
    }

    static String read(ClassLoader loader) {
        if (loader == null) {
            return UNKNOWN;
        }
        try (InputStream stream = loader.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return UNKNOWN;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String value = properties.getProperty("version");
            return value == null || value.isBlank() ? UNKNOWN : value.trim();
        } catch (IOException | RuntimeException error) {
            return UNKNOWN;
        }
    }
}
