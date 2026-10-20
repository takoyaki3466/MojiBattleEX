package jp.takoa.mojibattle.battle.event;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 多段数や弾数にかかわらず、通常攻撃の動作一回につき一回通知されるイベント。
 * 命中の有無や命中回数とは独立する。
 *
 * <h2>関係</h2>
 * <p>キャラクター定義を{@link ResourceId}、陣営を{@link Team}、個体区分を{@link AttackerType}で表す。
 * ホストの通常攻撃処理が生成し、
 * {@link jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime#onNormalAttack}から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * if (event.team() == NormalAttackEvent.Team.ALLY
 *         && event.attackerType() == NormalAttackEvent.AttackerType.MAIN) {
 *     // 味方メインの攻撃回数を記録する
 * }
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>投射物数ではなく「攻撃動作」を契機とするパッシブを、命中処理やcore攻撃者型へ依存せず実装するために作成された。</p>
 *
 * @param attackerInstanceId 戦闘内で一意な攻撃者個体ID
 * @param characterId 攻撃者のキャラクター定義ID
 * @param team 攻撃者の陣営
 * @param attackerType 攻撃者の個体区分
 */
public record NormalAttackEvent(String attackerInstanceId, ResourceId characterId, Team team, AttackerType attackerType) {
    public NormalAttackEvent {
        Objects.requireNonNull(attackerInstanceId, "attackerInstanceId");
        Objects.requireNonNull(characterId, "characterId");
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(attackerType, "attackerType");
    }
    /** 攻撃者が属する戦闘上の陣営。 */
    public enum Team { ALLY, ENEMY }
    /** 攻撃者がメイン、補助、召喚のどれであるかを表す。 */
    public enum AttackerType { MAIN, SUPPORT, SUMMON }
    public AttackType attackType() { return AttackType.NORMAL; }
    /** 将来のイベント共通化に備えた攻撃種別。現在は通常攻撃だけを表す。 */
    public enum AttackType { NORMAL }
}
