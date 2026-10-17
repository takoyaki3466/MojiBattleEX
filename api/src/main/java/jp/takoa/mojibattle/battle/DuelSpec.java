package jp.takoa.mojibattle.battle;

import java.util.List;
import java.util.Objects;

/** 一時的な決闘の参加者、対象制限およびダメージ補正。 */
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
