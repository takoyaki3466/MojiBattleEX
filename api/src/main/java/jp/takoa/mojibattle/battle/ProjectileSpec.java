package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * target方向へ生成する汎用Projectile定義。
 * @param damage 命中ごとのダメージ要求
 * @param speed 秒あたりの移動距離
 * @param radius 衝突半径
 * @param lifetimeSeconds 生成後の最大生存秒
 * @param projectileCount 同時に生成する弾数
 * @param spreadDegrees 弾列全体の扇角度
 * @param hitsPerProjectile 同方向へ重ねるhit数
 * @param textureId 任意の弾画像ID
 * @param effectId 任意の命中演出ID
 */
public record ProjectileSpec(DamageRequest damage, float speed, float radius, float lifetimeSeconds,
                             int projectileCount, float spreadDegrees, int hitsPerProjectile,
                             ResourceId textureId, ResourceId effectId) {
    public ProjectileSpec {
        Objects.requireNonNull(damage, "damage");
        if (!Float.isFinite(speed) || speed <= 0 || !Float.isFinite(radius) || radius <= 0
                || !Float.isFinite(lifetimeSeconds) || lifetimeSeconds <= 0
                || !Float.isFinite(spreadDegrees) || spreadDegrees < 0) {
            throw new IllegalArgumentException("Projectile dimensions must be finite and valid");
        }
        if (projectileCount < 1 || projectileCount > 64 || hitsPerProjectile < 1 || hitsPerProjectile > 64) {
            throw new IllegalArgumentException("Projectile counts must be between 1 and 64");
        }
    }
}
