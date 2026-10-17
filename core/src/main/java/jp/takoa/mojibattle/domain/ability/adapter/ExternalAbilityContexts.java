package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.AbilityActivationContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

final class ExternalAbilityContexts {
    private ExternalAbilityContexts() {
    }

    static AbilityActivationContext create(BattleController battle, CharacterRuntime user) {
        CharacterRuntime target = battle.targetOf(user.playerId);
        return new AbilityActivationContext(
                new CoreCombatantView(battle, user),
                target == null ? null : new CoreCombatantView(battle, target),
                new CoreCombatActions(battle));
    }
}
