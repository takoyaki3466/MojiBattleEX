package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

/** 敵の防御を暴落させ、配置済みの札束を一斉起爆する必殺技。 */
public final class CashCrashUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public CashCrashUltimate(AbilityConfigs.AreaUltimate config) {
        this.config = config;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions()
                || !user.isAlive()
                || user.isStunned()
                || !user.spendUltimate(config.pointCost)) {
            return false;
        }
        battle.detonateCashBundles(
                user,
                config.baseDamage,
                config.radius,
                config.warningSeconds,
                config.effectSeconds,
                config.defenseDownMultiplier,
                config.defenseDownDuration,
                config.objectTexture,
                config.warningEffectId,
                config.impactEffectId);
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
