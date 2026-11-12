package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.registry.RegistrySnapshot;
import jp.takoa.mojibattle.resource.ClasspathResourceManager;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ゲーム本体と全MODの登録、データ解決、Registry凍結を調停するComposition Root。
 */
public final class GameBootstrap {
    private GameBootstrap() {
    }

    /**
     * 標準クラスパスリソースだけを使う従来呼出し向け入口。
     */
    public static RegistrySnapshot bootstrap(Iterable<? extends GameMod> mods) {
        return bootstrap(mods, new ClasspathResourceManager(GameBootstrap.class.getClassLoader()));
    }

    /**
     * MODを指定順に登録し、既存JSONを解決してからRegistryを凍結する。
     * libGDXの内部ファイルAPIが利用可能になった後、起動につき一度だけ呼ぶ。
     *
     * @param mods 標準MODと依存解決済み外部MOD
     * @return 画面構築と戦闘開始に必要な共有サービス
     */
    public static RegistrySnapshot bootstrap(Iterable<? extends GameMod> mods, ResourceManager resources) {
        BootstrapProcess process = begin(mods, resources);
        try {
            process.runRemaining();
            return process.snapshot();
        } catch (RuntimeException error) {
            process.abort();
            throw error;
        }
    }

    /**
     * {@link #bootstrap(Iterable, ResourceManager)} と同じ処理を、段階ごとに進められる形で開始する。
     * 起動ロード画面がフレームの合間に処理を進めるために使う。
     *
     * @param mods 標準MODと依存解決済み外部MOD
     * @return まだ何も実行していない起動プロセス
     */
    public static BootstrapProcess begin(Iterable<? extends GameMod> mods, ResourceManager resources) {
        return new BootstrapProcess(validateMods(mods), resources);
    }

    private static List<GameMod> validateMods(Iterable<? extends GameMod> mods) {
        List<GameMod> ordered = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (GameMod mod : mods) {
            if (mod == null || mod.id() == null || mod.id().isBlank()) {
                throw new IllegalArgumentException("MOD and MOD ID are required");
            }
            if (!ids.add(mod.id())) {
                throw new IllegalArgumentException("Duplicate loaded MOD ID: " + mod.id());
            }
            ordered.add(mod);
        }
        return List.copyOf(ordered);
    }
}
