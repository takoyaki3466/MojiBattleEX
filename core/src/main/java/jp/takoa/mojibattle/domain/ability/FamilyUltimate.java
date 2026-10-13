package jp.takoa.mojibattle.domain.ability;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.FamilySummon;

/** 発動のたびに味方の家族ユニットを追加召喚する必殺技。 */
public final class FamilyUltimate implements IUltimate {
    private final AbilityConfigs.AreaUltimate config;

    public FamilyUltimate(AbilityConfigs.AreaUltimate config) {
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
        float phase = battle.nextAttackId() * 1.37f;
        for (int i = 0; i < config.summonCount; i++) {
            float angle = phase + MathUtils.PI2 * i / config.summonCount;
            float radius = 2.2f + (i % 2) * .8f;
            Vector3 spawn = new Vector3(user.position).add(
                    MathUtils.cos(angle) * radius,
                    0,
                    MathUtils.sin(angle) * radius);
            battle.spawn(new FamilySummon(
                    user.playerId,
                    spawn,
                    config.objectTexture,
                    config.summonHp,
                    config.summonDefense,
                    config.summonAttackDamage,
                    config.summonAttackInterval,
                    config.summonProjectileSpeed,
                    config.impactEffectIndex,
                    angle,
                    radius));
        }
        battle.emitImageEffect(
                new Vector3(user.position).add(0, 1.5f, 0),
                config.objectTexture,
                config.impactEffectIndex,
                8f,
                1.5f);
        battle.announceUltimate(user.playerId, config.nameKey);
        return true;
    }
}
