package jp.takoa.mojibattle.battle.damage;

import java.util.Objects;

/**
 * 正式な戦闘計算経路へ渡す、一回分の不変ダメージ要求。
 *
 * <h2>関係</h2>
 * <p>{@link DamageType}に依存し、各種action specが構成要素として保持する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#damage}が受け取り、ホストのダメージ計算が
 * 会心、攻撃種別倍率、防御無視を適用して{@link DamageResult}を返す。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * DamageRequest request = new DamageRequest(40f, DamageType.SKILL, false, true);
 * DamageResult result = actions.damage(user, target, request);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>引数の増加を抑え、即時・投射物・遅延攻撃のすべてで同じ計算指定を再利用するために作成された。</p>
 *
 * @param baseDamage              基礎ダメージ。有限かつ0以上
 * @param type                    攻撃種別
 * @param ignoreDefense           常に防御を無視するか
 * @param ignoreDefenseOnCritical 会心が成立した場合だけ防御を無視するか
 */
public record DamageRequest(float baseDamage, DamageType type, boolean ignoreDefense, boolean ignoreDefenseOnCritical) {
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
