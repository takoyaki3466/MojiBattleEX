package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.UltimateRuntime;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.IUltimate;

/** 公開UltimateRuntimeを既存の戦闘Ability契約へ接続する。 */
public final class ExternalUltimateAdapter implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    private final UltimateRuntime runtime;

    public ExternalUltimateAdapter(AbilityConfigs.AreaUltimate config, UltimateRuntime runtime) {
        this.config = config;
        this.runtime = runtime;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned()
                || user.ultimatePoints() < config.pointCost) {
            return false;
        }
        if (!runtime.tryActivate(ExternalAbilityContexts.create(battle, user))) {
            return false;
        }
        if (!user.spendUltimate(config.pointCost)) {
            throw new IllegalStateException("Ultimate points changed during external Ability activation");
        }
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
