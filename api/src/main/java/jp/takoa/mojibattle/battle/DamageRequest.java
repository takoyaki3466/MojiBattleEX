package jp.takoa.mojibattle.battle;

import java.util.Objects;

/**
 * 正式な戦闘計算経路へ渡す1回のダメージ要求。
 *
 * @param baseDamage 基礎ダメージ。有限かつ0以上
 * @param type 攻撃種別
 * @param ignoreDefense 常に防御を無視するか
 * @param ignoreDefenseOnCritical 会心が成立した場合だけ防御を無視するか
 */
public record DamageRequest(float baseDamage, DamageType type, boolean ignoreDefense,
                            boolean ignoreDefenseOnCritical) {
    public DamageRequest {
        if (!Float.isFinite(baseDamage) || baseDamage < 0) {
            throw new IllegalArgumentException("baseDamage must be finite and non-negative");
        }
        Objects.requireNonNull(type, "type");
    }

    public static DamageRequest of(float baseDamage, DamageType type) {
        return new DamageRequest(baseDamage, type, false, false);
    }
}
