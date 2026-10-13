package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.domain.combat.Projectile;

/** 補助キャラクターの追従移動、自動通常攻撃および能力発動を制御する。 */
public final class SupportController {
    private static final float ARRIVAL_TOLERANCE = .3f;
    private final SupportAbilityService abilities = new SupportAbilityService();

    /** 補助キャラクターを更新し、味方追従と射程内の自動攻撃を処理する。 */
    public void update(
            BattleController battle,
            SupportCharacterRuntime support,
            CharacterRuntime ally,
            CharacterRuntime enemy,
            float delta) {
        support.update(delta);
        if (support.skillReady()) {
            abilities.useSkill(battle, battle.team(support.owner));
        }

        float side = support.owner.ordinal() % 2 == 0
                ? -support.definition.followDistance
                : support.definition.followDistance;
        Vector3 desired = new Vector3(ally.position)
                .mulAdd(new Vector3(-ally.facing.z, 0, ally.facing.x), side);
        Vector3 movement = desired.sub(support.position);
        movement.y = 0;
        float distance = movement.len();
        if (distance > ARRIVAL_TOLERANCE) {
            float travelDistance = Math.min(
                    support.definition.moveSpeed * battle.fieldMoveMultiplier() * delta,
                    distance);
            support.position.mulAdd(movement.nor(), travelDistance);
        }
        support.position.y = ally.position.y;

        float attackRangeSquared = support.definition.attackRange * support.definition.attackRange;
        if (support.attackReady() && support.position.dst2(enemy.position) <= attackRangeSquared) {
            Vector3 start = new Vector3(support.position).add(0, 1, 0);
            Vector3 velocity = new Vector3(enemy.position)
                    .add(0, 1, 0)
                    .sub(start)
                    .nor()
                    .scl(support.definition.projectileSpeed);
            Projectile shot = new Projectile(
                    battle.nextAttackId(),
                    support.owner,
                    support.definition.displayGlyph,
                    support.definition.projectileTexture,
                    6,
                    start,
                    velocity,
                    .42f,
                    support.definition.baseDamage,
                    0,
                    2.5f).criticalProfile(
                            support.definition.criticalEnabled,
                            support.definition.criticalRate,
                            support.definition.criticalDamageMultiplier);
            if (support.definition.curvedProjectile) {
                shot.curve(24f);
            }
            battle.spawn(shot);
            support.resetAttack();
        }
    }

    public void useSkill(BattleController battle, PlayerTeam team) {
        abilities.useSkill(battle, team);
    }

    public void useUltimate(BattleController battle, PlayerTeam team) {
        abilities.useUltimate(battle, team);
    }
}
