package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.domain.support.CoreSupportActions;
import jp.takoa.mojibattle.domain.support.SupportAbilityDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

/**
 * core補助コンテキストを公開型だけのコンテキストへ変換する。
 */
public final class ExternalSupportAbilityContexts {
    private ExternalSupportAbilityContexts() {
    }

    public static SupportAbilityActivationContext create(BattleController battle, PlayerTeam team, SupportCharacterRuntime support, SupportAbilityDefinition ability) {
        CharacterRuntime target = battle.targetOf(team.main().playerId);

        return new SupportAbilityActivationContext(new CoreSupportView(support), new CoreCombatantView(battle, team.main()), target == null ? null : new CoreCombatantView(battle, target), new CoreCombatActions(battle), MapConfigView.of(ability), battle.elapsedSeconds(), new CoreSupportActions(battle, support), support.state());
    }
}
