package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;
import jp.takoa.mojibattle.registry.EffectIds;

/**
 * 補助キャラクターの追従移動、自動通常攻撃および能力発動を制御する。
 */
public final class SupportController {
    private static final float ARRIVAL_TOLERANCE = .3f;
    private final SupportAbilityService abilities;

    public SupportController(Registry<SupportAbilityImplementation> implementations) {
        abilities = new SupportAbilityService(implementations);
    }

    /**
     * 補助キャラクターを更新し、味方追従と射程内の自動攻撃を処理する。
     */
    public void update(
            BattleController battle,
            SupportCharacterRuntime support,
            CharacterRuntime ally,
            CharacterRuntime enemy,
            int supportIndex,
            int supportCount,
            float delta) {
        support.update(delta, battle.supportUltimateChargeMultiplier());

        if (support.skillReady()) {
            abilities.useSkill(battle, battle.team(support.owner), support);
        }

        float centeredIndex = supportIndex - (Math.max(1, supportCount) - 1) * .5f;
        float side = centeredIndex * support.definition.followDistance * .9f;
        float back = 2.7f + Math.abs(centeredIndex) * .35f;
        Vector3 desired = new Vector3(ally.position)
                .mulAdd(new Vector3(-ally.facing.z, 0, ally.facing.x), side)
                .mulAdd(ally.facing, -back);
        Vector3 movement = desired.sub(support.position);
        movement.y = 0;

        float distance = movement.len();

        if (distance > ARRIVAL_TOLERANCE) {
            float travelDistance = Math.min(support.definition.moveSpeed * battle.fieldMoveMultiplier(support) * delta, distance);
            support.position.mulAdd(movement.nor(), travelDistance);
        }

        support.position.y = ally.position.y;

        float attackRangeSquared = support.definition.attackRange * support.definition.attackRange;

        if (support.attackReady() && support.position.dst2(enemy.position) <= attackRangeSquared) {
            Vector3 start = new Vector3(support.position).add(0, 1, 0);
            Vector3 velocity = new Vector3(enemy.position).add(0, 1, 0).sub(start).nor().scl(support.definition.projectileSpeed);

            Projectile shot = new Projectile(battle.nextAttackId(), support.owner, support.definition.displayGlyph, support.definition.projectileTexture, EffectIds.WIND, start, velocity, .42f, support.definition.baseDamage, 0, 2.5f).criticalProfile(support.definition.criticalEnabled, support.definition.criticalRate, support.definition.criticalDamageMultiplier);

            if (support.definition.curvedProjectile) {
                shot.curve(24f);
            }

            battle.spawn(shot);
            support.resetAttack();
        }
    }

    public void useSkill(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        abilities.useSkill(battle, team, support);
    }

    public void useUltimate(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        abilities.useUltimate(battle, team, support);
    }
}
