package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 対象方向へ一発以上を生成する汎用投射物仕様。
 *
 * <h2>関係</h2>
 * <p>正式な命中計算を{@link DamageRequest}、表示資源を{@link ResourceId}で指定する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnProjectile}へ渡され、ホストの投射物移動と衝突処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnProjectile(user, target,
 *         new ProjectileSpec(damage, 10f, 0.25f, 3f, 3, 20f, 1, texture, hitFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>単発、扇状複数弾、同方向多段を一つの仕様で扱い、最も一般的な遠距離攻撃を簡潔に記述するために作成された。</p>
 *
 * @param damage            命中ごとのダメージ要求
 * @param speed             秒あたりの移動距離
 * @param radius            衝突半径
 * @param lifetimeSeconds   生成後の最大生存秒
 * @param projectileCount   同時に生成する弾数
 * @param spreadDegrees     弾列全体の扇角度
 * @param hitsPerProjectile 同方向へ重ねるhit数
 * @param textureId         任意の弾画像ID
 * @param effectId          任意の命中演出ID
 */
public record ProjectileSpec(DamageRequest damage, float speed, float radius, float lifetimeSeconds,
                             int projectileCount, float spreadDegrees, int hitsPerProjectile, ResourceId textureId,
                             ResourceId effectId) {
    public ProjectileSpec {
        Objects.requireNonNull(damage, "damage");

        if (!Float.isFinite(speed) || speed <= 0 || !Float.isFinite(radius) || radius <= 0 || !Float.isFinite(lifetimeSeconds) || lifetimeSeconds <= 0 || !Float.isFinite(spreadDegrees) || spreadDegrees < 0) {
            throw new IllegalArgumentException("Projectile dimensions must be finite and valid");
        }
        if (projectileCount < 1 || projectileCount > 64 || hitsPerProjectile < 1 || hitsPerProjectile > 64) {
            throw new IllegalArgumentException("Projectile counts must be between 1 and 64");
        }
    }
}
