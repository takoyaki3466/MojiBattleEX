package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 対象の現在位置を生成時に固定し、遅延後に範囲内の敵へ適用する円形攻撃仕様。
 *
 * <h2>関係</h2>
 * <p>各対象への計算を{@link DamageRequest}で指定し、演出資源を{@link ResourceId}で参照する。
 * Abilityが構築して{@link jp.takoa.mojibattle.battle.action.CombatActions#spawnAreaAttack}へ渡し、ホストの範囲攻撃管理が消費する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnAreaAttack(user, target,
 *         new AreaAttackSpec(0.8f, 3f, damage, warningFx, impactFx, null, 1f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>予告、待機、範囲判定、命中をホストへ委譲し、Abilityが独自タイマーを持たずに遅延範囲攻撃を表現するために作成された。</p>
 *
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
