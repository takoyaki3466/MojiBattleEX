package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 対象を追跡し、指定時間後に正式なダメージ経路を実行する単体攻撃仕様。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}に依存し、演出と待機中オブジェクトを{@link ResourceId}で参照する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnDelayedStrike}へ渡され、ホストの遅延攻撃管理が対象の生存と現在位置を追跡する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnDelayedStrike(user, target,
 *         new DelayedStrikeSpec(1f, damage, impactFx, markerTexture, 1f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>単純な遅延処理をRuntimeの可変タイマーとして実装せず、戦闘終了や対象消滅時の後始末をホストへ集約するために作成された。</p>
 *
 * @param delaySeconds 発動までの非負秒
 * @param damage 適用するダメージ要求
 * @param effectId 任意の発動演出ID
 * @param objectTexture 任意の待機中表示画像ID
 * @param effectScale 非負の演出倍率
 */
public record DelayedStrikeSpec(float delaySeconds, DamageRequest damage, ResourceId effectId,
                                ResourceId objectTexture, float effectScale) {
    public DelayedStrikeSpec {
        if (!Float.isFinite(delaySeconds) || delaySeconds < 0) {
            throw new IllegalArgumentException("delaySeconds must be finite and non-negative");
        }
        Objects.requireNonNull(damage, "damage");
        if (!Float.isFinite(effectScale) || effectScale < 0) {
            throw new IllegalArgumentException("effectScale must be finite and non-negative");
        }
    }
}
