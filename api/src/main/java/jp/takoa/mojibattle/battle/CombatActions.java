package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * Ability実装がcore内部型へ触れずに要求できる戦闘操作の最小境界。
 */
public interface CombatActions {
    void damage(CombatantView source, CombatantView target, float amount);

    /** DamageService相当の全計算を通して、指定種別の基礎ダメージを与える。 */
    default void damage(CombatantView source, CombatantView target, float amount, DamageType type) {
        damage(source, target, amount);
    }

    /** 構造化要求を正式なダメージ計算へ流し、確定結果を返す。 */
    default DamageResult damage(CombatantView source, CombatantView target, DamageRequest request) {
        throw new UnsupportedOperationException("Structured damage is not supported by this combat host");
    }

    void heal(CombatantView target, float amount);

    /** 最大HPに対する割合を回復する。1は最大HPと同量を表す。 */
    default void healRatio(CombatantView target, float maximumHpRatio) {
        if (!Float.isFinite(maximumHpRatio) || maximumHpRatio < 0) {
            throw new IllegalArgumentException("maximumHpRatio must be finite and non-negative");
        }
        heal(target, target.maximumHp() * maximumHpRatio);
    }

    /** 通常の獲得として必殺ポイントを加算する。 */
    default void addUltimatePoints(CombatantView target, float points) {
        throw new UnsupportedOperationException("Ultimate point changes are not supported by this combat host");
    }

    /** 直前の消費を取り消す用途で、獲得倍率を適用せず必殺ポイントを返す。 */
    default void refundUltimatePoints(CombatantView target, float points) {
        throw new UnsupportedOperationException("Ultimate point refunds are not supported by this combat host");
    }

    /** 対象へ状態を付与する。最終能力値は使用時に再評価される。 */
    default void applyStatus(CombatantView source, CombatantView target, StatusSpec status) {
        throw new UnsupportedOperationException("Statuses are not supported by this combat host");
    }

    /** 指定時間後に対象へ攻撃する戦闘オブジェクトを生成する。 */
    default void spawnDelayedStrike(CombatantView source, CombatantView target, DelayedStrikeSpec spec) {
        throw new UnsupportedOperationException("Delayed strikes are not supported by this combat host");
    }

    default void spawnProjectile(CombatantView source, CombatantView target, ProjectileSpec spec) {
        throw new UnsupportedOperationException("Projectiles are not supported by this combat host");
    }

    default void spawnAreaAttack(CombatantView source, CombatantView target, AreaAttackSpec spec) {
        throw new UnsupportedOperationException("Area attacks are not supported by this combat host");
    }

    void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds);

    /** 使用者と同じチームおよび死亡者を除いた敵を返す。 */
    default List<CombatantView> enemiesOf(CombatantView source) {
        return List.of();
    }

    /** 使用者自身を含む、同じチームの生存メインキャラクターを登録順で返す。 */
    default List<CombatantView> alliesOf(CombatantView source) {
        return List.of(source);
    }

    /** sourceからtargetを離す。実装は戦場境界とキャラクター衝突を保証する。 */
    default void knockbackAwayFrom(CombatantView source, CombatantView target, float distance) {
        throw new UnsupportedOperationException("Knockback is not supported by this combat host");
    }
}
