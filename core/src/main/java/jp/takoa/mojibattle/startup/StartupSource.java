package jp.takoa.mojibattle.startup;

import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.util.List;

/**
 * 起動ロード画面が使う、プラットフォーム側（デスクトップランチャー等）のMOD供給元。
 *
 * <p>メソッドはGLスレッドから {@link #runtime()}、{@link #discoverMods()}、{@link #resources()} の順に呼ばれる。
 * 起動に失敗した場合や中断された場合、{@link #runtime()} が返したランタイムは起動ロード側が閉じる。
 * 成功した場合はゲーム共有サービス（{@code GameContext}）へ引き渡され、ゲーム終了時に閉じられる。</p>
 */
public interface StartupSource {
    /**
     * 標準MODと外部MODを探索し、読込み順に返す。起動につき一度だけ呼ばれる。
     */
    List<GameMod> discoverMods();

    /**
     * {@link #discoverMods()} の後に、MODのリソースを含むリソース層を返す。
     */
    ResourceManager resources();

    /**
     * ゲーム終了時に閉じる外部ランタイム（MOD ClassLoader等）。不要な場合はnull。
     * 探索前に呼ばれ、探索に失敗した場合も閉じられる。
     */
    AutoCloseable runtime();
}
