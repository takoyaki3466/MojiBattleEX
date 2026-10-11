package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.*;

public final class SpinningOneUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    public SpinningOneUltimate(AbilityConfigs.AreaUltimate config) { this.config = config; }
    @Override public String id() { return config.id; }
    @Override public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || !user.spendUltimate(config.pointCost)) return false;
        battle.spawn(new SpinningOne(user.playerId, user.position, config.lineLength, config.lineWidth,
            config.baseDamage, config.healAmount, config.effectSeconds, config.objectTexture, config.impactEffectIndex));
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
