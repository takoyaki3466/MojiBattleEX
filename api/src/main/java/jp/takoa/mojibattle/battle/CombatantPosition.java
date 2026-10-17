package jp.takoa.mojibattle.battle;

/** ライブラリ非依存の戦場座標。 */
public record CombatantPosition(float x, float y, float z) {
    public static final CombatantPosition ORIGIN = new CombatantPosition(0, 0, 0);

    public CombatantPosition {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Combatant position must be finite");
        }
    }
}
