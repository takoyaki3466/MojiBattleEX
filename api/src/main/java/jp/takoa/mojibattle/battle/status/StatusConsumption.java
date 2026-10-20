package jp.takoa.mojibattle.battle.status;

/**
 * 状態効果を一度だけ消費して解除する戦闘契機。
 *
 * <h2>関係</h2>
 * <p>{@link StatusSpec}が一つ保持し、ホストのダメージ適用処理から参照される。
 * {@link StatusStacking}や{@link StatusDuration}とは独立して、条件成立時の即時解除だけを指定する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * StatusConsumption consumption = StatusConsumption.ON_DAMAGE_RECEIVED;
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>「次に受ける一回だけ」の効果を特殊なRuntimeとして実装せず、状態定義の一部としてホストが原子的に消費できるようにするために作成された。</p>
 */
public enum StatusConsumption {
    NONE,
    ON_DAMAGE_RECEIVED
}
