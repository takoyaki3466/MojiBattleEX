package jp.takoa.mojibattle.item.bootstrap;

import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.registry.RegistrySnapshot;

/**
 * 旧呼出し元を標準MODだけの新しいcore Bootstrapへ委譲する互換ファサード。
 */
@Deprecated
public final class GameBootstrap {
    private GameBootstrap() {
    }

    /**
     * 登録宣言、データ読込、参照検証、実行Ability生成、Registry凍結を順に実行する。
     * libGDXの内部ファイルAPIが利用可能になった後、アプリケーション起動につき一度だけ呼ぶ。
     *
     * @return 画面構築と対戦開始に必要な共有サービス一式
     * @throws RuntimeException 登録、JSON、翻訳、アセット、または参照の検証に失敗した場合
     */
    public static RegistrySnapshot bootstrap() {
        return jp.takoa.mojibattle.bootstrap.GameBootstrap.bootstrap(
                java.util.List.of(new ItemContentModule()));
    }
}
