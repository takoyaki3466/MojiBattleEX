package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

/** 必殺ポイントを消費し、使用者のHPを回復する必殺技。 */
public final class SelfHealUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public SelfHealUltimate(AbilityConfigs.AreaUltimate config) {
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
        user.heal(config.healAmount);
        battle.emitImageEffect(
                new Vector3(user.position).add(0, 1, 0),
                config.objectTexture,
                config.impactEffectId,
                5f,
                1.2f);
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
