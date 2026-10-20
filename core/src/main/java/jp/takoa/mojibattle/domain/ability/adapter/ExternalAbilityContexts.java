package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

public final class ExternalAbilityContexts {
    private ExternalAbilityContexts() {
    }

    public static AbilityActivationContext create(BattleController battle, CharacterRuntime user) {
        CharacterRuntime target = battle.targetOf(user.playerId);
        return new AbilityActivationContext(
                new CoreCombatantView(battle, user),
                target == null ? null : new CoreCombatantView(battle, target),
                new CoreCombatActions(battle));
    }
}
