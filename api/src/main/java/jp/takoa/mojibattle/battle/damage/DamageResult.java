package jp.takoa.mojibattle.battle.damage;

/**
 * ホストの計算とHP適用が完了した後の、実ダメージと成立特性。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}に対する結果として
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#damage}から返される。
 * Runtimeは追加回復や分岐に利用できるが、結果を変更することはできない。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * DamageResult result = actions.damage(user, target, request);
 * if (result.critical()) actions.heal(user, result.dealtDamage() * 0.1f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>基礎値と実際に減ったHPを区別し、Ability側で防御・会心計算を再現する必要をなくすために作成された。</p>
 *
 * @param dealtDamage    HPへ実際に適用された非負ダメージ
 * @param critical       会心判定が成立したか
 * @param defenseIgnored 防御倍率を迂回したか
 */
public record DamageResult(float dealtDamage, boolean critical, boolean defenseIgnored) {
    public DamageResult {
        if (!Float.isFinite(dealtDamage) || dealtDamage < 0) {
            throw new IllegalArgumentException("dealtDamage must be finite and non-negative");
        }
    }
}
