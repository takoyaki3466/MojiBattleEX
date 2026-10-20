package jp.takoa.mojibattle.battle.status;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * MODが宣言する状態効果の不変定義。
 *
 * <h2>関係</h2>
 * <p>識別子の{@link ResourceId}、効果種別の{@link StatusEffectType}、期間の{@link StatusDuration}、
 * 再付与規則の{@link StatusStacking}、消費契機の{@link StatusConsumption}に依存する。
 * Abilityが構築して{@link jp.takoa.mojibattle.battle.action.CombatActions#applyStatus}へ渡し、
 * ホストの状態コンテナと各戦闘計算から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * StatusSpec buff = StatusSpec.multiplier(id,
 *         StatusEffectType.ATTACK_MULTIPLIER, 1.25f,
 *         StatusDuration.seconds(8f));
 * actions.applyStatus(user, user, buff);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>状態の識別、期間、重複、周期、消費を一つの不変値として渡し、Abilityとホストで状態管理規則が分散しないようにするために作成された。</p>
 *
 * @param id 同じ付与元からの再付与判定に使う名前空間付きID
 * @param type 変更する戦闘値
 * @param magnitude 倍率または加算量
 * @param duration 有限または永続の期間
 * @param stacking 同一ID再付与時の規則
 * @param maximumStacks 最大スタック数
 * @param tickIntervalSeconds 継続効果の正の周期秒
 * @param consumption 一度だけ消費する条件
 */
public record StatusSpec(ResourceId id, StatusEffectType type, float magnitude, StatusDuration duration,
                         StatusStacking stacking, int maximumStacks, float tickIntervalSeconds,
                         StatusConsumption consumption) {
    public StatusSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(stacking, "stacking");
        Objects.requireNonNull(consumption, "consumption");
        if (!Float.isFinite(magnitude)
                || type != StatusEffectType.CRITICAL_RATE_ADDITION && magnitude < 0) {
            throw new IllegalArgumentException("magnitude must be finite; multipliers and periodic amounts must be non-negative");
        }
        if (maximumStacks < 1) {
            throw new IllegalArgumentException("maximumStacks must be at least 1");
        }
        if (!Float.isFinite(tickIntervalSeconds) || tickIntervalSeconds <= 0) {
            throw new IllegalArgumentException("tickIntervalSeconds must be finite and positive");
        }
    }

    public StatusSpec(ResourceId id, StatusEffectType type, float magnitude, StatusDuration duration,
                      StatusStacking stacking, int maximumStacks, float tickIntervalSeconds) {
        this(id, type, magnitude, duration, stacking, maximumStacks, tickIntervalSeconds,
                StatusConsumption.NONE);
    }

    public static StatusSpec multiplier(ResourceId id, StatusEffectType type, float magnitude,
                                        StatusDuration duration) {
        return new StatusSpec(id, type, magnitude, duration, StatusStacking.REFRESH, 1, 1f,
                StatusConsumption.NONE);
    }
}
