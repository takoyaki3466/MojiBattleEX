package jp.takoa.mojibattle.battle.action;

import jp.takoa.mojibattle.battle.action.spec.*;
import jp.takoa.mojibattle.battle.combatant.*;
import jp.takoa.mojibattle.battle.damage.*;
import jp.takoa.mojibattle.battle.presentation.*;
import jp.takoa.mojibattle.battle.rule.DuelSpec;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * Ability実装がcore内部型へ触れずに副作用を要求する、戦闘操作のファサード。
 * このインターフェースを通した呼出しだけが、Abilityからホストの戦闘状態を変更する。
 *
 * <h2>関係</h2>
 * <p>{@link CombatantView}と{@link BattlefieldView}を対象指定・照会に使用し、
 * {@link DamageRequest}/{@link DamageResult}、{@link StatusSpec}、action spec、
 * {@link CutInSpec}/{@link DomainSpec}、{@link DuelSpec}を受け取る。
 * Abilityの各contextがこの境界を保持し、ホストの戦闘アダプターが実装する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * DamageRequest request = DamageRequest.of(25f, DamageType.SKILL);
 * actions.damage(user, target, request);
 * actions.spawnProjectile(user, target,
 *         new ProjectileSpec(request, 8f, 0.4f, 3f, 1, 0f, 1, null, null));
 * }</pre>
 *
 * <h2>互換性と実装規約</h2>
 * <p>基礎操作以外はdefaultメソッドとして追加される。未対応ホストは
 * {@link UnsupportedOperationException}を返すため、API拡張時に既存実装を即座に壊さない。
 * 実装は渡された戦闘個体が自身の戦闘に属することを検証し、時間更新や衝突処理をホスト側で管理する。</p>
 *
 * <h2>設計理由</h2>
 * <p>Capability-styleの境界により、itemモジュールをcoreから完全に切り離しながら、戦闘操作をテスト用ホストへ差し替えられるようにするために作成された。</p>
 */
public interface CombatActions {
    /** メイン、補助、召喚を含む現在の戦場。 */
    default BattlefieldView battlefield() {
        return BattlefieldView.EMPTY;
    }

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

    /**
     * 現在の対象へ状態を付与できるかを返す。
     *
     * <p>既定実装は生存中のメイン・補助を対象とする。召喚の状態対応はホストごとに異なるため、
     * 対応するホストはこのメソッドを上書きする。{@link #applyStatus(CombatantView, CombatantView, StatusSpec)}
     * はこの照会結果にかかわらず不正な対象を検証し、例外として通知しなければならない。</p>
     */
    default boolean canApplyStatus(CombatantView target) {
        return target != null && target.alive() && target.kind() != CombatantKind.SUMMON;
    }

    /** 生存中の敵から、現在状態を付与できる個体だけを登録順で返す。 */
    default List<CombatantView> statusEffectEnemiesOf(CombatantView source) {
        return enemiesOf(source).stream().filter(this::canApplyStatus).toList();
    }

    /** 生存中の味方から、現在状態を付与できる個体だけを登録順で返す。 */
    default List<CombatantView> statusEffectAlliesOf(CombatantView source) {
        return alliesOf(source).stream().filter(this::canApplyStatus).toList();
    }

    /** 指定時間後に対象へ攻撃する戦闘オブジェクトを生成する。 */
    default void spawnDelayedStrike(CombatantView source, CombatantView target, DelayedStrikeSpec spec) {
        throw new UnsupportedOperationException("Delayed strikes are not supported by this combat host");
    }

    default void spawnProjectile(CombatantView source, CombatantView target, ProjectileSpec spec) {
        throw new UnsupportedOperationException("Projectiles are not supported by this combat host");
    }

    default void spawnProjectilePattern(CombatantView source, ProjectilePatternSpec spec) {
        throw new UnsupportedOperationException("Projectile patterns are not supported by this combat host");
    }

    default void spawnAreaAttack(CombatantView source, CombatantView target, AreaAttackSpec spec) {
        throw new UnsupportedOperationException("Area attacks are not supported by this combat host");
    }

    default void spawnFallingStrike(CombatantView source, FallingStrikeSpec spec) {
        throw new UnsupportedOperationException("Falling strikes are not supported by this combat host");
    }

    default void spawnLineAttack(CombatantView source, LineAttackSpec spec) {
        throw new UnsupportedOperationException("Line attacks are not supported by this combat host");
    }

    default void summonCompanions(CombatantView source, CompanionSummonSpec spec) {
        throw new UnsupportedOperationException("Companion summons are not supported by this combat host");
    }

    default void spawnMovingHazards(CombatantView source, MovingHazardSpec spec) {
        throw new UnsupportedOperationException("Moving hazards are not supported by this combat host");
    }

    default void spawnSpinningAttack(CombatantView source, SpinningAttackSpec spec) {
        throw new UnsupportedOperationException("Spinning attacks are not supported by this combat host");
    }

    default void startSkillBarrage(CombatantView source, SkillBarrageSpec spec) {
        throw new UnsupportedOperationException("Skill barrages are not supported by this combat host");
    }

    default void startProjectileBarrage(CombatantView source, ProjectileBarrageSpec spec) {
        throw new UnsupportedOperationException("Projectile barrages are not supported by this combat host");
    }

    default void detonateOwnedMarkers(CombatantView source, DetonationSpec spec) {
        throw new UnsupportedOperationException("Marker detonation is not supported by this combat host");
    }

    /** Spawns one persistent marker for later detonation, up to the per-owner limit. */
    default boolean spawnDetonatableMarker(CombatantView source, ResourceId texture, int maximumOwned) {
        throw new UnsupportedOperationException("Detonatable markers are not supported by this combat host");
    }

    /** Applies action lock directly; status duration multipliers remain host-controlled. */
    default void stun(CombatantView target, float durationSeconds) {
        throw new UnsupportedOperationException("Stun is not supported by this combat host");
    }

    default void bind(CombatantView target, float durationSeconds) {
        throw new UnsupportedOperationException("Bind is not supported by this combat host");
    }

    default void pullToward(CombatantView target, CombatantView destination, float maximumDistance,
                            float remainingDistance) {
        throw new UnsupportedOperationException("Pull movement is not supported by this combat host");
    }

    default boolean teleportBesideTarget(CombatantView mover, float distance) {
        throw new UnsupportedOperationException("Target-relative teleport is not supported by this combat host");
    }

    default void halveAllSkillCooldowns() {
        throw new UnsupportedOperationException("Global cooldown changes are not supported by this combat host");
    }

    default void gatherMainCombatantsAtCenter(float centerScale) {
        throw new UnsupportedOperationException("Battlefield gathering is not supported by this combat host");
    }

    /** Half-size of the square arena used for coordinate-based Ability placement. */
    default float arenaHalfExtent() {
        return 0f;
    }

    /** Extra attacks contributed by the source's current passive runtimes. */
    default long additionalUltimateAttackCount(CombatantView source) {
        return 0L;
    }

    void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds);

    /** 使用者と同じチームおよび死亡者を除いた敵を返す。 */
    default List<CombatantView> enemiesOf(CombatantView source) {
        return battlefield().enemiesOf(source);
    }

    /** 使用者自身を含む、同じチームの生存メインキャラクターを登録順で返す。 */
    default List<CombatantView> alliesOf(CombatantView source) {
        List<CombatantView> allies = battlefield().alliesOf(source);
        return allies.isEmpty() ? List.of(source) : allies;
    }

    /** 指定条件の決闘を開始する。 */
    default void startDuel(DuelSpec spec) {
        throw new UnsupportedOperationException("Duels are not supported by this combat host");
    }

    /** 攻撃者の通常攻撃対象を指定秒数だけ固定する。補助・召喚も攻撃者に指定できる。 */
    default void lockNormalAttackTarget(CombatantView attacker, CombatantView target, float durationSeconds) {
        throw new UnsupportedOperationException("Normal attack target locks are not supported by this combat host");
    }

    /** sourceからtargetを離す。実装は戦場境界とキャラクター衝突を保証する。 */
    default void knockbackAwayFrom(CombatantView source, CombatantView target, float distance) {
        throw new UnsupportedOperationException("Knockback is not supported by this combat host");
    }

    /** 視覚領域を追加する。同時発動中の別領域や同一領域を解除しない。 */
    default void activateDomain(CombatantView source, DomainSpec spec) {
        throw new UnsupportedOperationException("Domains are not supported by this combat host");
    }

    /** 指定した使用者が発動した領域が一つ以上残っているかを返す。 */
    default boolean isDomainActive(CombatantView source, ResourceId domainId) {
        return false;
    }

    /** 優先度付きの全画面カットインを要求する。 */
    default void showCutIn(CombatantView source, CutInSpec spec) {
        throw new UnsupportedOperationException("Cut-ins are not supported by this combat host");
    }

    /** 一定時間続く中央吸引と周期攻撃を開始する。 */
    default void startBattlefieldPulse(CombatantView source, BattlefieldPulseSpec spec) {
        throw new UnsupportedOperationException("Battlefield pulses are not supported by this combat host");
    }

    /** 2体の現在位置を戦場境界内で交換する。 */
    default void swapPositions(CombatantView first, CombatantView second) {
        throw new UnsupportedOperationException("Position swapping is not supported by this combat host");
    }

    /** 対象を戦場中央に対して点対称の位置へ移す。 */
    default void mirrorAcrossArenaCenter(CombatantView target) {
        throw new UnsupportedOperationException("Arena mirroring is not supported by this combat host");
    }

    /** 対象へ付与されている負の状態と行動不能を解除する。 */
    default void cleanseNegativeStatuses(CombatantView target) {
        throw new UnsupportedOperationException("Status cleansing is not supported by this combat host");
    }
}
