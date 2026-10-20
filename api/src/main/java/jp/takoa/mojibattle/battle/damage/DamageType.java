package jp.takoa.mojibattle.battle.damage;

/**
 * ホストの戦闘計算で適用する攻撃種別固有の倍率とイベント経路を選択する。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}が必ず一つ保持し、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#damage}の構造化ダメージ処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * DamageRequest request = DamageRequest.of(30f, DamageType.ULTIMATE);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Ability側で倍率を複製せず、通常・スキル・必殺技の補正規則をホストへ集約するために作成された。</p>
 */
public enum DamageType {
    NORMAL,
    SKILL,
    ULTIMATE
}
