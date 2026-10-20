package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.combatant.CombatantPosition;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 生成時に固定した座標へ落下し、任意で継続炎上を残す攻撃仕様。
 *
 * <h2>関係</h2>
 * <p>着弾点は{@link CombatantPosition}、直接ダメージは{@link DamageRequest}、画像と演出は{@link ResourceId}で表す。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnFallingStrike}へ渡され、ホストの落下物・予告・炎上更新処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * FallingStrikeSpec strike = new FallingStrikeSpec(target.position(), 2f, damage,
 *         0.8f, 0.4f, 8f, warningFx, impactFx, "meteor", texture,
 *         0f, 3f, 2f, 0.5f, 0f, 0f);
 * actions.spawnFallingStrike(user, strike);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>落下物の複数段階と炎上を一つのホスト管理ライフサイクルにまとめ、座標固定型攻撃を再利用できるようにするために作成された。</p>
 *
 * @param position 生成時に固定する着弾座標
 * @param radius 着弾判定半径
 * @param damage 着弾ダメージ要求
 * @param warningSeconds 予告秒
 * @param effectSeconds 着弾演出秒
 * @param spawnHeight 表示上の落下開始高さ
 * @param warningEffectId 任意の予告演出ID
 * @param impactEffectId 任意の着弾演出ID
 * @param dropStyle ホストが解釈する落下表示形式
 * @param objectTexture 任意の落下物画像ID
 * @param horizontalOffset 着弾点に対する横方向表示オフセット
 * @param burnDuration 炎上を残す秒数。0なら炎上なし
 * @param burnDamage 一周期ごとの炎上ダメージ
 * @param burnTickInterval 炎上周期秒
 * @param burnUltimatePointsPerTick 炎上一周期ごとの必殺ポイント
 * @param startDelaySeconds 落下開始前の追加待機秒
 */
public record FallingStrikeSpec(CombatantPosition position, float radius, DamageRequest damage,
                                float warningSeconds, float effectSeconds, float spawnHeight,
                                ResourceId warningEffectId, ResourceId impactEffectId,
                                String dropStyle, ResourceId objectTexture, float horizontalOffset,
                                float burnDuration, float burnDamage, float burnTickInterval,
                                float burnUltimatePointsPerTick, float startDelaySeconds) {
    public FallingStrikeSpec {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(damage, "damage");
        if (!positive(radius) || !nonNegative(warningSeconds) || !nonNegative(effectSeconds)
                || !nonNegative(spawnHeight) || !nonNegative(horizontalOffset)
                || !nonNegative(burnDuration) || !nonNegative(burnDamage)
                || !nonNegative(burnTickInterval) || !nonNegative(burnUltimatePointsPerTick)
                || !nonNegative(startDelaySeconds)) {
            throw new IllegalArgumentException("Falling strike values must be finite and non-negative");
        }
    }

    private static boolean positive(float value) { return Float.isFinite(value) && value > 0; }
    private static boolean nonNegative(float value) { return Float.isFinite(value) && value >= 0; }
}
