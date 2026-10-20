package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 一定時間、敵を戦場中央へ引き寄せながら周期ダメージを与える戦場パルス仕様。
 *
 * <h2>関係</h2>
 * <p>周期ダメージは{@link DamageRequest}、継続演出は{@link ResourceId}で指定する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#startBattlefieldPulse}へ渡され、ホストのパルス更新処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.startBattlefieldPulse(user,
 *         new BattlefieldPulseSpec(5f, 0.5f, 2f, damage, effectId, 1.2f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>全敵の移動と周期判定を単一のホスト管理オブジェクトへまとめ、Abilityごとの走査処理を不要にするために作成された。</p>
 *
 * @param durationSeconds 効果を維持する正の秒数
 * @param tickIntervalSeconds ダメージ判定の正の周期秒
 * @param pullUnitsPerSecond 敵を中央へ移動させる秒間距離
 * @param damagePerTick 一周期ごとのダメージ要求
 * @param effectId 継続演出ID
 * @param effectScale 演出倍率
 */
public record BattlefieldPulseSpec(float durationSeconds, float tickIntervalSeconds,
                                   float pullUnitsPerSecond, DamageRequest damagePerTick,
                                   ResourceId effectId, float effectScale) {
    public BattlefieldPulseSpec {
        Objects.requireNonNull(damagePerTick, "damagePerTick");
        Objects.requireNonNull(effectId, "effectId");
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0
                || !Float.isFinite(tickIntervalSeconds) || tickIntervalSeconds <= 0
                || !Float.isFinite(pullUnitsPerSecond) || pullUnitsPerSecond < 0
                || !Float.isFinite(effectScale) || effectScale < 0) {
            throw new IllegalArgumentException("Battlefield pulse values are invalid");
        }
    }
}
