package jp.takoa.mojibattle.battle;

/** 同じIDかつ同じ付与元の状態を再付与した際の規則。 */
public enum StatusStacking {
    /** 効果を新しい定義で置き換える。 */
    REPLACE,
    /** 効果量は変えず残り時間を長い方へ更新する。 */
    REFRESH,
    /** 最大数までスタックを1増やし、残り時間も更新する。 */
    ADD_STACK
}
