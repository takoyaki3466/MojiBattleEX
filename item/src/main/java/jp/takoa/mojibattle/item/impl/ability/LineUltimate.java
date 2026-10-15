package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.LineStrike;

/** 使用者の向きを基準に複数方向の直線攻撃を生成する必殺技。 */
public final class LineUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public LineUltimate(AbilityConfigs.AreaUltimate config) {
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
        int directions = Math.max(1, config.directions);
        for (int i = 0; i < directions; i++) {
            Vector3 direction = directions == 1
                    ? new Vector3(user.facing)
                    : new Vector3(
                            MathUtils.cos(MathUtils.PI2 * i / directions),
                            0,
                            MathUtils.sin(MathUtils.PI2 * i / directions));
            battle.spawn(new LineStrike(
                    user.playerId,
                    user.position,
                    direction,
                    config.lineLength,
                    config.lineWidth,
                    config.baseDamage,
                    config.warningSeconds + i * .035f,
                    config.effectSeconds,
                    config.warningEffectId,
                    config.impactEffectId,
                    config.objectTexture).mainUltimate());
        }
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
