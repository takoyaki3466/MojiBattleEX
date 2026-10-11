package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.UltimateStrike;

public final class BankruptcyUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;
    public BankruptcyUltimate(AbilityConfigs.AreaUltimate config) { this.config = config; }
    public String id() { return config.id; }
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || user.ultimatePoints() < config.pointCost) return false;
        CharacterRuntime target = battle.targetOf(user.playerId);if(target==null)return false;
        if (!user.spendUltimate(config.pointCost)) return false;
        battle.spawn(new UltimateStrike(user.playerId, target.position, config.radius, config.baseDamage,
            config.warningSeconds, config.effectSeconds, config.spawnHeight, config.warningEffectIndex, config.impactEffectIndex,
            config.dropStyle, config.objectTexture, config.fallHorizontalOffset,config.burnDuration,config.burnDamage,config.burnTickInterval,config.burnUltimatePointsPerTick));
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
