package jp.takoa.mojibattle.battle;

/**
 * Ability実行時に公開する使用者、対象、戦闘操作の組。
 */
public record AbilityContext(CombatantView user, CombatantView target, CombatActions actions) {
    public AbilityContext {
        if (user == null || actions == null) {
            throw new IllegalArgumentException("Ability context requires a user and actions");
        }
    }
}
