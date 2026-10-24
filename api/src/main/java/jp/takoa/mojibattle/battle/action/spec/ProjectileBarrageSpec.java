package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 使用者から一定間隔で弾を連続発射する、時間制御付き弾幕仕様。
 *
 * <h2>関係</h2>
 * <p>命中計算に{@link DamageRequest}、弾と演出に{@link ResourceId}を使用する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#startProjectileBarrage}へ渡され、ホストの弾幕スケジューラーと投射物管理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.startProjectileBarrage(user,
 *         new ProjectileBarrageSpec(8, 0.15f, damage, 10f, 3f, 0.25f,
 *                 texture, hitFx, "強", 1.2f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>複数フレームにまたがる連射をホストが中断・破棄できるようにし、Runtimeにスケジューラーを持たせないために作成された。</p>
 *
 * @param count               総発射数
 * @param intervalSeconds     発射間隔秒
 * @param damage              一発ごとのダメージ要求
 * @param speed               弾の秒間速度
 * @param lifetimeSeconds     一発ごとの最大生存秒
 * @param radius              一発ごとの衝突半径
 * @param texture             任意の弾画像ID
 * @param impactEffectId      任意の命中演出ID
 * @param enhancedNormalGlyph 強化通常攻撃を示す任意の文字
 * @param enhancedNormalScale 強化表示倍率。0なら追加表示なし
 */
public record ProjectileBarrageSpec(int count, float intervalSeconds, DamageRequest damage, float speed,
                                    float lifetimeSeconds, float radius, ResourceId texture, ResourceId impactEffectId,
                                    String enhancedNormalGlyph, float enhancedNormalScale) {
    public ProjectileBarrageSpec {
        Objects.requireNonNull(damage, "damage");

        if (count < 1 || count > 256 || !positive(intervalSeconds) || !positive(speed) || !positive(lifetimeSeconds) || !positive(radius) || !Float.isFinite(enhancedNormalScale) || enhancedNormalScale < 0) {
            throw new IllegalArgumentException("Projectile barrage values are invalid");
        }
    }

    private static boolean positive(float value) {
        return Float.isFinite(value) && value > 0;
    }
}
