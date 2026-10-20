package jp.takoa.mojibattle.battle.status;

/**
 * 公開状態効果が変更できる戦闘値または周期処理。倍率は1を無変更値とする。
 *
 * <h2>関係</h2>
 * <p>{@link StatusSpec}が一つ保持し、ホストの能力値集計、ダメージ計算、クールダウン、周期更新から参照される。
 * Abilityはこの列挙値を選び、計算式の実装はホストへ委譲する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * StatusSpec haste = StatusSpec.multiplier(id,
 *         StatusEffectType.SKILL_COOLDOWN_MULTIPLIER, 0.8f,
 *         StatusDuration.seconds(6f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>任意コードを状態として注入せず、ホストが安全かつ決定的に合成できる効果の種類をAPIで制限するために作成された。</p>
 */
public enum StatusEffectType {
    ATTACK_MULTIPLIER,
    INCOMING_DAMAGE_MULTIPLIER,
    MOVE_SPEED_MULTIPLIER,
    CRITICAL_RATE_ADDITION,
    CRITICAL_DAMAGE_MULTIPLIER,
    ULTIMATE_GAIN_MULTIPLIER,
    SKILL_COOLDOWN_MULTIPLIER,
    NORMAL_ATTACK_INTERVAL_MULTIPLIER,
    DAMAGE_OVER_TIME,
    HEAL_OVER_TIME,
    NEXT_INCOMING_DAMAGE_MULTIPLIER,
    BONUS_DAMAGE_MULTIPLIER
}
