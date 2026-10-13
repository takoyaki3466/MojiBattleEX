package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.SpinningOne;

/** 巨大な「1」を回転させ、命中に応じた回復を行う必殺技。 */
public final class SpinningOneUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public SpinningOneUltimate(AbilityConfigs.AreaUltimate config) {
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
        battle.spawn(new SpinningOne(
                user.playerId,
                user.position,
                config.lineLength,
                config.lineWidth,
                config.baseDamage,
                config.healAmount,
                user.missingHp(),
                config.effectSeconds,
                config.objectTexture,
                config.impactEffectIndex));
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
