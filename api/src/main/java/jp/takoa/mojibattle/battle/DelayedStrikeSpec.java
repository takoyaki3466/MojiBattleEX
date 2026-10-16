package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 対象を追跡し、指定時間後に正式なダメージ経路を実行する攻撃定義。
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
