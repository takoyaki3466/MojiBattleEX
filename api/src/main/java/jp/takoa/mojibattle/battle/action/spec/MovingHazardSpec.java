package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 使用者から扇状に放出する、移動式接触ハザードの仕様。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}と表示用{@link ResourceId}に依存し、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnMovingHazards}へ渡される。
 * ホストの移動物・衝突・行動不能管理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnMovingHazards(user,
 *         new MovingHazardSpec(3, 4f, 0.6f, damage, 1f, 5f, texture, impactFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>ダメージとスタンを伴う移動障害物を共通化し、Runtimeから衝突状態と寿命管理を分離するために作成された。</p>
 *
 * @param count           生成数。1から64
 * @param speed           秒あたりの移動距離
 * @param radius          接触半径
 * @param damage          接触時ダメージ要求
 * @param stunSeconds     接触対象のスタン秒
 * @param lifetimeSeconds 最大生存秒
 * @param texture         任意のハザード画像ID
 * @param impactEffectId  任意の接触演出ID
 */
public record MovingHazardSpec(int count, float speed, float radius, DamageRequest damage, float stunSeconds,
                               float lifetimeSeconds, ResourceId texture, ResourceId impactEffectId) {
    public MovingHazardSpec {
        Objects.requireNonNull(damage, "damage");

        if (count < 1 || count > 64 || !positive(speed) || !positive(radius) || !nonNegative(stunSeconds) || !positive(lifetimeSeconds)) {
            throw new IllegalArgumentException("Moving hazard values are invalid");
        }
    }

    private static boolean positive(float value) {
        return Float.isFinite(value) && value > 0;
    }

    private static boolean nonNegative(float value) {
        return Float.isFinite(value) && value >= 0;
    }
}
