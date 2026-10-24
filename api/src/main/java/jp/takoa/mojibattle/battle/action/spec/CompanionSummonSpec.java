package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.resource.ResourceId;

/**
 * Abilityが生成する、使用者の周囲を追従する味方召喚個体の共通仕様。
 *
 * <h2>関係</h2>
 * <p>画像と命中演出を{@link ResourceId}で参照する。
 * Abilityが構築し、{@link jp.takoa.mojibattle.battle.action.CombatActions#summonCompanions}へ渡し、
 * ホストの召喚・移動・通常攻撃処理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.summonCompanions(user,
 *         new CompanionSummonSpec(2, texture, 40f, 0.8f, 6f, 1f, 8f, hitFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>召喚物の内部クラスを公開せず、数と基本能力だけで一貫した味方召喚を要求できるようにするために作成された。</p>
 *
 * @param count           生成数。1から64
 * @param texture         召喚個体の画像ID。ホスト既定表示を使う場合は{@code null}
 * @param maximumHp       一体ごとの最大HP
 * @param defense         一体ごとの防御値
 * @param attackDamage    通常攻撃の基礎ダメージ
 * @param attackInterval  通常攻撃間隔秒
 * @param projectileSpeed 通常攻撃弾の秒間速度
 * @param impactEffectId  任意の命中演出ID
 */
public record CompanionSummonSpec(int count, ResourceId texture, float maximumHp, float defense, float attackDamage,
                                  float attackInterval, float projectileSpeed, ResourceId impactEffectId) {
    public CompanionSummonSpec {
        if (count < 1 || count > 64 || !positive(maximumHp) || !nonNegative(defense) || !positive(attackDamage) || !positive(attackInterval) || !positive(projectileSpeed)) {
            throw new IllegalArgumentException("Companion summon values are invalid");
        }
    }

    private static boolean positive(float value) {
        return Float.isFinite(value) && value > 0;
    }

    private static boolean nonNegative(float value) {
        return Float.isFinite(value) && value >= 0;
    }
}
