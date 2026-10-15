package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.PlayerTeam;

/**
 * 登録済み補助能力ハンドラへ渡す戦闘コンテキスト。
 */
public final class SupportAbilityContext {
    public final BattleController battle;
    public final PlayerTeam team;
    public final SupportCharacterRuntime support;
    public final SupportAbilityDefinition ability;

    public SupportAbilityContext(BattleController battle, PlayerTeam team, SupportCharacterRuntime support, SupportAbilityDefinition ability) {
        this.battle = battle;
        this.team = team;
        this.support = support;
        this.ability = ability;
    }
}
