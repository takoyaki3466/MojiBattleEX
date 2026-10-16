package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * targetの現在位置を生成時に固定し、遅延後に範囲内の敵へ適用する攻撃。
 * @param delaySeconds 発動までの非負秒
 * @param radius 攻撃半径
 * @param damage 各対象へ適用するダメージ要求
 * @param warningEffectId 任意の予告演出ID
 * @param impactEffectId 任意の命中演出ID
 * @param objectTexture 任意の表示オブジェクト画像ID
 * @param effectScale 非負の演出倍率
 */
public record AreaAttackSpec(float delaySeconds, float radius, DamageRequest damage,
                             ResourceId warningEffectId, ResourceId impactEffectId,
                             ResourceId objectTexture, float effectScale) {
    public AreaAttackSpec {
        Objects.requireNonNull(damage, "damage");
        if (!Float.isFinite(delaySeconds) || delaySeconds < 0 || !Float.isFinite(radius) || radius <= 0
                || !Float.isFinite(effectScale) || effectScale < 0) {
            throw new IllegalArgumentException("Area attack timing, radius, or scale is invalid");
        }
    }
}
