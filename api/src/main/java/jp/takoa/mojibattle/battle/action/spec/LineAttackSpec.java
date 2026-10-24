package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 使用者から等角度に放射する、一方向以上の矩形直線攻撃仕様。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}と演出用{@link ResourceId}に依存し、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnLineAttack}へ渡される。
 * ホストは方向数に応じた予告領域、範囲判定、描画を生成する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.spawnLineAttack(user,
 *         new LineAttackSpec(8f, 1f, damage, 0.5f, 0.3f, 4, warningFx, hitFx, texture));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>十字・放射状攻撃を同じ仕様で表し、Abilityが角度計算と個別衝突判定を実装しないようにするために作成された。</p>
 *
 * @param length          使用者からの攻撃長
 * @param width           各直線の幅
 * @param damage          各命中対象へのダメージ要求
 * @param warningSeconds  判定前の予告秒
 * @param effectSeconds   命中演出秒
 * @param directions      等角度に配置する方向数
 * @param warningEffectId 任意の予告演出ID
 * @param impactEffectId  任意の命中演出ID
 * @param objectTexture   任意の直線オブジェクト画像ID
 */
public record LineAttackSpec(float length, float width, DamageRequest damage, float warningSeconds, float effectSeconds,
                             int directions, ResourceId warningEffectId, ResourceId impactEffectId,
                             ResourceId objectTexture) {
    public LineAttackSpec {
        Objects.requireNonNull(damage, "damage");

        if (!Float.isFinite(length) || length <= 0 || !Float.isFinite(width) || width <= 0 || !Float.isFinite(warningSeconds) || warningSeconds < 0 || !Float.isFinite(effectSeconds) || effectSeconds < 0 || directions < 1 || directions > 64) {
            throw new IllegalArgumentException("Line attack values are invalid");
        }
    }
}
