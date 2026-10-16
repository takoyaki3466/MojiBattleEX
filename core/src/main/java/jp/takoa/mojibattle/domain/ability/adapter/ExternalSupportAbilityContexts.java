package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.SupportAbilityActivationContext;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.support.SupportAbilityContext;

/** core補助コンテキストを公開型だけのコンテキストへ変換する。 */
public final class ExternalSupportAbilityContexts {
    private ExternalSupportAbilityContexts() {
    }

    public static SupportAbilityActivationContext create(SupportAbilityContext context) {
        CharacterRuntime target = context.battle.targetOf(context.team.main().playerId);
        return new SupportAbilityActivationContext(
                new CoreSupportView(context.support),
                new CoreCombatantView(context.team.main()),
                target == null ? null : new CoreCombatantView(target),
                new CoreCombatActions(context.battle),
                MapConfigView.of(context.ability.parameters),
                context.battle.elapsedSeconds());
    }
}
