package jp.takoa.mojibattle.crash;

/**
 * クラッシュレポートへ記録する大まかなゲーム状態。
 */
public enum GameState {
    /** libGDXと共有コンテキストの初期化中（起動ロード画面を含む）。 */
    STARTUP,
    /** タイトル、設定、キャラクター選択などのメニュー画面。 */
    MENU,
    /** キャラクター選択完了後、戦闘開始前のロード画面。 */
    LOADING,
    /** 戦闘画面。 */
    BATTLE,
    /** 結果画面。 */
    RESULT
}
