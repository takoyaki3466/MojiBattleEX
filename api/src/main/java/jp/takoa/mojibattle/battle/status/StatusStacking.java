package jp.takoa.mojibattle.battle.status;

/**
 * 同じIDかつ同じ付与元の状態を再付与した際の合成規則。
 *
 * <h2>関係</h2>
 * <p>{@link StatusSpec}が一つ保持し、ホストの状態登録処理が既存スタックとの統合に使用する。
 * 最大スタック数と残り時間は{@link StatusSpec}および{@link StatusDuration}から得る。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * StatusStacking rule = StatusStacking.ADD_STACK;
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>再付与時の暗黙挙動をなくし、置換・時間更新・積層をAbility定義側から明示できるようにするために作成された。</p>
 */
public enum StatusStacking {
    /**
     * 効果を新しい定義で置き換える。
     */
    REPLACE,
    /**
     * 効果量は変えず残り時間を長い方へ更新する。
     */
    REFRESH,
    /**
     * 最大数までスタックを1増やし、残り時間も更新する。
     */
    ADD_STACK
}
