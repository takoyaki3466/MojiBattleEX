package jp.takoa.mojibattle.domain;

/**
 * 最大4人のプレイヤーを安定した列挙順で識別するID。
 */
public enum PlayerId {
    ONE,
    TWO,
    THREE,
    FOUR;

    /**
     * 列挙順で次に位置するプレイヤーを返す。
     *
     * <p>末尾のプレイヤーでは先頭へ循環する。対象選択の初期候補として利用される。</p>
     *
     * @return 次のプレイヤーID
     */
    public PlayerId opponent() {
        return values()[(ordinal() + 1) % values().length];
    }

    /**
     * UIに表示する「1P」から「4P」までのラベルを返す。
     *
     * @return プレイヤー番号の表示文字列
     */
    public String label() {
        return (ordinal() + 1) + "P";
    }
}
