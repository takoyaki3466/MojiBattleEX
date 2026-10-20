package jp.takoa.mojibattle.battle.rule;

import jp.takoa.mojibattle.battle.combatant.CombatantView;
import java.util.List;
import java.util.Objects;

/**
 * 一時的な決闘へ参加する個体、対象制限、ダメージ補正をまとめたルール仕様。
 *
 * <h2>関係</h2>
 * <p>参加者を{@link CombatantView}で指定し、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#startDuel}へ渡す。
 * ホストの対象選択、ダメージ計算、決闘時間管理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.startDuel(new DuelSpec(List.of(user), target, 10f,
 *         true, 0.8f, 0.5f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>一時的な対象制限と参加内外の補正を同時に開始・終了し、Abilityが複数システムの解除順序を管理しなくて済むようにするために作成された。</p>
 *
 * @param allies 決闘へ参加する一体以上の味方
 * @param enemy 決闘対象の敵
 * @param durationSeconds 決闘を維持する正の秒数
 * @param restrictTargets 参加者同士以外を通常攻撃・スキル対象から除くか
 * @param participantDefenseMultiplier 参加者が受けるダメージへ適用する非負倍率
 * @param outsideAttackerDamageMultiplier 決闘外から参加者へのダメージへ適用する非負倍率
 */
public record DuelSpec(List<CombatantView> allies, CombatantView enemy, float durationSeconds,
                       boolean restrictTargets, float participantDefenseMultiplier,
                       float outsideAttackerDamageMultiplier) {
    public DuelSpec {
        allies = List.copyOf(Objects.requireNonNull(allies, "allies"));
        if (allies.isEmpty() || allies.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Duel requires at least one allied combatant");
        }
        Objects.requireNonNull(enemy, "enemy");
        requirePositiveFinite(durationSeconds, "durationSeconds");
        requireNonNegativeFinite(participantDefenseMultiplier, "participantDefenseMultiplier");
        requireNonNegativeFinite(outsideAttackerDamageMultiplier, "outsideAttackerDamageMultiplier");
    }

    private static void requirePositiveFinite(float value, String name) {
        if (!Float.isFinite(value) || value <= 0) throw new IllegalArgumentException(name);
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException(name);
    }
}
