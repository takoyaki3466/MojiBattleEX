package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 通常攻撃へ合成する不変補正。個数deltaは基準値1へ加算し、倍率は全補正を乗算する。
 * 非数値値はpriorityが高いもの、同値ならID辞書順が後のものを採用する。
 * @param id 決定的な同順位比較にも使う補正ID
 * @param priority 画像・演出選択の優先順位
 * @param attackIntervalMultiplier 攻撃間隔へ乗算する正数
 * @param projectileCountDelta 基準弾数1への加算値
 * @param spreadDegrees 弾列全体の扇角度
 * @param hitsPerAttackDelta 基準hit数1への加算値
 * @param damageMultiplier 通常攻撃ダメージ倍率
 * @param projectileTexture 任意の弾画像ID
 * @param effectId 任意の発射・命中演出ID
 */
public record NormalAttackModifier(ResourceId id, int priority, float attackIntervalMultiplier,
                                   int projectileCountDelta, float spreadDegrees,
                                   int hitsPerAttackDelta, float damageMultiplier,
                                   ResourceId projectileTexture, ResourceId effectId) {
    public NormalAttackModifier {
        Objects.requireNonNull(id, "id");
        if (!Float.isFinite(attackIntervalMultiplier) || attackIntervalMultiplier <= 0
                || !Float.isFinite(spreadDegrees) || spreadDegrees < 0
                || !Float.isFinite(damageMultiplier) || damageMultiplier < 0) {
            throw new IllegalArgumentException("Normal attack multipliers and spread are invalid");
        }
        if (projectileCountDelta < -64 || hitsPerAttackDelta < -64) {
            throw new IllegalArgumentException("Normal attack count deltas are invalid");
        }
    }
}
