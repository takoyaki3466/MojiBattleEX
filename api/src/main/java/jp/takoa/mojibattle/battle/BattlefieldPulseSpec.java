package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 一定時間、敵を戦場中央へ引き寄せながら周期ダメージを与える汎用戦場パルス。
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
