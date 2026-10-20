package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 使用者を中心に追従しながら回転する直線攻撃仕様。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}と任意の{@link ResourceId}に依存し、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnSpinningAttack}へ渡される。
 * ホストの追従オブジェクト・回転衝突・命中回復処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnSpinningAttack(user,
 *         new SpinningAttackSpec(5f, 0.8f, damage, 2f, 0.5f, 4f, texture, hitFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>使用者の移動へ追従する継続判定、回転、命中回復を一つのライフサイクルとしてホストへ委譲するために作成された。</p>
 *
 * @param length 回転線の長さ
 * @param width 回転線の幅
 * @param damage 命中ごとのダメージ要求
 * @param healOnHit 一命中ごとの固定回復量
 * @param missingHealthBonus 減少HPに応じた追加回復係数
 * @param lifetimeSeconds 攻撃を維持する秒数
 * @param texture 任意の攻撃画像ID
 * @param impactEffectId 任意の命中演出ID
 */
public record SpinningAttackSpec(float length, float width, DamageRequest damage, float healOnHit,
                                 float missingHealthBonus, float lifetimeSeconds,
                                 ResourceId texture, ResourceId impactEffectId) {
    public SpinningAttackSpec {
        Objects.requireNonNull(damage, "damage");
        if (!positive(length) || !positive(width) || !nonNegative(healOnHit)
                || !nonNegative(missingHealthBonus) || !positive(lifetimeSeconds)) {
            throw new IllegalArgumentException("Spinning attack values are invalid");
        }
    }
    private static boolean positive(float value) { return Float.isFinite(value) && value > 0; }
    private static boolean nonNegative(float value) { return Float.isFinite(value) && value >= 0; }
}
