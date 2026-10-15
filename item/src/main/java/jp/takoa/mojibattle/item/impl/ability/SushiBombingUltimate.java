package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.SushiBarrage;

/** 一定間隔で寿司投射物を連続発射する弾幕型必殺技。 */
public final class SushiBombingUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public SushiBombingUltimate(AbilityConfigs.AreaUltimate config) {
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
        battle.spawn(new SushiBarrage(
                user,
                config.barrageCount,
                config.barrageInterval,
                config.baseDamage,
                config.projectileSpeed,
                config.projectileLifetimeSeconds,
                config.projectileRadius,
                config.objectTexture,
                config.impactEffectId,
                config.ignoreDefense));
        user.enhanceNormalAttack("寿司", config.objectTexture, .45f);
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
