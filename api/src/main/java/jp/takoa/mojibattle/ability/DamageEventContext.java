package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;

import java.util.Objects;

/**
 * 確定ダメージ後に攻撃側・被弾側パッシブへ通知する読取りコンテキスト。
 * @param source ダメージの発生元
 * @param target ダメージを受けた参加者
 * @param dealtDamage HPへ実際に適用された非負ダメージ
 * @param actions 同じ戦闘に対する操作境界
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record DamageEventContext(CombatantView source, CombatantView target, float dealtDamage,
                                 CombatActions actions, float battleElapsedSeconds) {
    public DamageEventContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(actions, "actions");
        if (!Float.isFinite(dealtDamage) || dealtDamage < 0) {
            throw new IllegalArgumentException("dealtDamage must be finite and non-negative");
        }
    }
}
