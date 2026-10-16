package jp.takoa.mojibattle.battle;

/**
 * 確定後の実ダメージと、その計算で成立した特性。
 * @param dealtDamage HPへ実際に適用された非負ダメージ
 * @param critical 会心判定が成立したか
 * @param defenseIgnored 防御倍率を迂回したか
 */
public record DamageResult(float dealtDamage, boolean critical, boolean defenseIgnored) {
    public DamageResult {
        if (!Float.isFinite(dealtDamage) || dealtDamage < 0) {
            throw new IllegalArgumentException("dealtDamage must be finite and non-negative");
        }
    }
}
