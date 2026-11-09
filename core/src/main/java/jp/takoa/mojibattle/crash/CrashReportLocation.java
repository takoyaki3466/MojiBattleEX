package jp.takoa.mojibattle.crash;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * クラッシュレポートの保存先を実行環境から解決する。
 *
 * <p>優先順位:</p>
 * <ol>
 *     <li>{@code -Dmojibattle.crashReportDir}（明示指定。Gradleの開発実行で使用）</li>
 *     <li>jpackageランチャーが設定する {@code jpackage.app-path} の親ディレクトリ直下の {@code crush-report}</li>
 *     <li>jpackage設定で渡す {@code -Dmojibattle.appDir=$APPDIR} の親（アプリケーションイメージのルート）直下の {@code crush-report}</li>
 *     <li>IDE実行時、クラスの配置場所から遡って見つかるGradleプロジェクトの {@code build/crush-report}</li>
 * </ol>
 * <p>上記へ書き込めない場合や解決できない場合は、ユーザーが書き込める代替保存先を使用する。
 * 現在の作業ディレクトリ（{@code user.dir}）は保存先として使用しない。</p>
 */
public final class CrashReportLocation {
    /** 指定どおりのフォルダ名。{@code crash-report} へ変更しないこと。 */
    public static final String DIRECTORY_NAME = "crush-report";
    static final String OVERRIDE_PROPERTY = "mojibattle.crashReportDir";
    static final String JPACKAGE_APP_PATH_PROPERTY = "jpackage.app-path";
    static final String APP_DIR_PROPERTY = "mojibattle.appDir";

    private final Path primary;
    private final List<Path> fallbacks;

    CrashReportLocation(Path primary, List<Path> fallbacks) {
        this.primary = primary;
        this.fallbacks = List.copyOf(fallbacks);
    }

    /**
     * 現在のJVMのシステムプロパティ、環境変数、クラス配置場所から保存先を解決する。
     */
    public static CrashReportLocation detect() {
        return resolve(CrashReportLocation::systemProperty, CrashReportLocation::environment, codeSourceOf(CrashReportLocation.class));
    }

    /**
     * テスト可能な形で保存先を解決する。
     *
     * @param properties システムプロパティの参照
     * @param environment 環境変数の参照
     * @param codeSource このクラスを含むJARまたはクラスディレクトリ。不明ならnull
     */
    static CrashReportLocation resolve(Function<String, String> properties, Function<String, String> environment, Path codeSource) {
        Path primary = firstNonNull(
                path(properties.apply(OVERRIDE_PROPERTY)),
                applicationImageRootFromLauncher(path(properties.apply(JPACKAGE_APP_PATH_PROPERTY))),
                applicationImageRootFromAppDir(path(properties.apply(APP_DIR_PROPERTY))),
                developmentDirectory(codeSource));

        Set<Path> fallbacks = new LinkedHashSet<>();
        Path localAppData = path(environment.apply("LOCALAPPDATA"));
        if (localAppData != null) {
            fallbacks.add(localAppData.resolve("MojiBattleEX").resolve(DIRECTORY_NAME));
        }
        Path home = path(properties.apply("user.home"));
        if (home != null) {
            fallbacks.add(home.resolve(".mojibattleex").resolve(DIRECTORY_NAME));
        }
        Path temporary = path(properties.apply("java.io.tmpdir"));
        if (temporary != null) {
            fallbacks.add(temporary.resolve("MojiBattleEX").resolve(DIRECTORY_NAME));
        }
        if (primary != null) {
            fallbacks.remove(primary);
        }
        return new CrashReportLocation(primary, new ArrayList<>(fallbacks));
    }

    /** 通常時の保存先。解決できなかった場合はnull。 */
    public Path primary() {
        return primary;
    }

    /** 通常の保存先へ書き込めない場合に順に試す保存先。 */
    public List<Path> fallbacks() {
        return fallbacks;
    }

    /** 試行順に並べた全保存先。 */
    public List<Path> candidates() {
        List<Path> all = new ArrayList<>();
        if (primary != null) {
            all.add(primary);
        }
        all.addAll(fallbacks);
        return all;
    }

    private static Path applicationImageRootFromLauncher(Path launcher) {
        if (launcher == null) {
            return null;
        }
        Path root = launcher.toAbsolutePath().normalize().getParent();
        return root == null ? null : root.resolve(DIRECTORY_NAME);
    }

    private static Path applicationImageRootFromAppDir(Path appDirectory) {
        if (appDirectory == null) {
            return null;
        }
        Path root = appDirectory.toAbsolutePath().normalize().getParent();
        return root == null ? null : root.resolve(DIRECTORY_NAME);
    }

    /**
     * クラス配置場所の祖先にGradleのsettingsファイルがあれば、そのプロジェクトのbuild配下を使う。
     * 配布物ではsettingsファイルが存在しないため選択されない。
     */
    static Path developmentDirectory(Path codeSource) {
        if (codeSource == null) {
            return null;
        }
        Path current = codeSource.toAbsolutePath().normalize();
        for (int depth = 0; current != null && depth < 8; depth++, current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("settings.gradle.kts")) || Files.isRegularFile(current.resolve("settings.gradle"))) {
                return current.resolve("build").resolve(DIRECTORY_NAME);
            }
        }
        return null;
    }

    private static Path codeSourceOf(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            return source == null || source.getLocation() == null ? null : Path.of(source.getLocation().toURI());
        } catch (URISyntaxException | RuntimeException error) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Path path(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value.trim());
        } catch (InvalidPathException error) {
            return null;
        }
    }

    private static String systemProperty(String name) {
        try {
            return System.getProperty(name);
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static String environment(String name) {
        try {
            return System.getenv(name);
        } catch (RuntimeException error) {
            return null;
        }
    }
}
