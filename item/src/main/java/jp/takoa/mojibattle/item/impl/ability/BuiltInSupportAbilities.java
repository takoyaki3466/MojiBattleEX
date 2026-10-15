package jp.takoa.mojibattle.item.impl.ability;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.domain.support.SupportAbilityContext;
import jp.takoa.mojibattle.registry.EffectIds;

import java.util.List;

/** 組み込み補助スキル・補助必殺の具体的な戦闘効果。 */
public final class BuiltInSupportAbilities {
    private BuiltInSupportAbilities() {
    }

    public static void applyAllyAttackUp(SupportAbilityContext context) {
        context.team.main().statuses.apply(new StatusEffect(
                context.ability.id, context.team.main().playerId, StatusType.ATTACK_UP,
                1 + context.ability.magnitude, context.ability.durationSeconds));
        context.battle.emitImageEffect(
                context.team.main().position, "glyphs/t.png", EffectIds.STAR, 8, 1.1f);
    }

    public static void applyEnemyDefenseDown(SupportAbilityContext context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id, context.team.main().playerId,
                    StatusType.DAMAGE_TAKEN_UP, 1 + context.ability.magnitude,
                    context.ability.durationSeconds));
            Vector3 side = new Vector3(-target.facing.z, 0, target.facing.x);
            context.battle.emitEffect(
                    new Vector3(target.position).mulAdd(side, 3), EffectIds.STAR, 9, 1.5f);
            context.battle.emitEffect(
                    new Vector3(target.position).mulAdd(side, -3), EffectIds.STAR, 9, 1.5f);
        }
    }

    public static void applyEnemyAttackDown(SupportAbilityContext context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id, context.team.main().playerId, StatusType.ATTACK_DOWN,
                    1 - context.ability.magnitude, context.ability.durationSeconds));
            context.battle.emitImageEffect(
                    target.position, "glyphs/zero.png", EffectIds.IMPACT, 7, 1.2f);
        }
    }

    public static void applyHealCleanse(SupportAbilityContext context) {
        context.team.main().heal(
                context.team.main().maxHp() * context.ability.magnitude);
        context.team.main().clearNegativeStatuses();
        context.battle.emitImageEffect(
                context.team.main().position, "glyphs/zero.png", EffectIds.IMPACT, 11, 1.5f);
    }

    public static void applyBonusDamage(SupportAbilityContext context) {
        context.team.main().statuses.apply(new StatusEffect(
                context.ability.id, context.team.main().playerId, StatusType.BONUS_DAMAGE,
                1 + context.ability.magnitude, context.ability.durationSeconds));
        context.battle.emitImageEffect(
                context.team.main().position, "glyphs/slash.png", EffectIds.CRITICAL, 9, 1.1f);
    }

    public static void applyWideSlash(SupportAbilityContext context) {
        context.battle.spawn(new LineStrike(
                context.team.main().playerId,
                context.support.position,
                new Vector3(enemy(context).position).sub(context.support.position),
                context.ability.range,
                3.5f,
                context.ability.baseDamage,
                .25f,
                .8f,
                EffectIds.SPEED,
                EffectIds.CRITICAL,
                "objects/energy-lance.png"));
    }

    public static void applyEnemySlow(SupportAbilityContext context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id, context.team.main().playerId, StatusType.MOVE_DOWN,
                    1 - context.ability.magnitude, context.ability.durationSeconds));
            context.battle.emitEffect(target.position, EffectIds.SPEED, 6, 1);
        }
    }

    public static void applyRightTeleport(SupportAbilityContext context) {
        boolean teleported = context.battle.teleportRight(
                context.team.main().playerId, context.ability.range);
        if (teleported) {
            context.team.main().statuses.apply(new StatusEffect(
                    context.ability.id, context.team.main().playerId, StatusType.ATTACK_UP,
                    1 + context.ability.magnitude, context.ability.durationSeconds));
        } else {
            context.support.addUltimatePoints(context.ability.pointCost);
        }
    }

    public static void applyWindPull(SupportAbilityContext context) {
        for (CharacterRuntime target : enemies(context)) {
            Vector3 direction = new Vector3(context.team.main().position).sub(target.position);
            direction.y = 0;
            float distance = direction.len();
            if (distance > 0) {
                target.position.mulAdd(
                        direction.scl(1f / distance),
                        Math.min(context.ability.range, Math.max(0, distance - 2f)));
            }
            target.applyBind(context.ability.durationSeconds);
            context.battle.emitImageEffect(
                    target.position, "glyphs/matsu.png", EffectIds.VEHICLE, 9, 1.25f);
        }
    }

    public static void applyGlobalCooldownHalve(SupportAbilityContext context) {
        context.battle.halveAllSkillCooldowns();
        for (CharacterRuntime target : context.battle.players()) {
            context.battle.emitImageEffect(
                    target.position, "glyphs/matsu.png", EffectIds.FAMILY, 10, 1.5f);
        }
    }

    public static void applyTrainCharge(SupportAbilityContext context) {
        float extent = context.battle.arenaHalfExtent();
        boolean leftToRight = context.team.main().playerId.ordinal() % 2 == 0;
        Vector3 start = new Vector3(
                leftToRight ? -extent + .5f : extent - .5f,
                0,
                context.team.main().position.z);
        context.battle.spawn(new CarHazard(
                context.team.main().playerId,
                start,
                new Vector3(leftToRight ? 1 : -1, 0, 0),
                context.ability.range,
                2.1f,
                context.ability.baseDamage,
                context.ability.magnitude,
                context.ability.durationSeconds,
                extent,
                "objects/commuter-train.png",
                EffectIds.VEHICLE));
        context.battle.emitImageEffect(
                start, "objects/commuter-train.png", EffectIds.VEHICLE, 12f, .8f);
    }

    public static void applyGatherAll(SupportAbilityContext context) {
        for (CharacterRuntime target : context.battle.players()) {
            target.position.scl(.12f);
            target.position.y = 0;
            context.battle.emitImageEffect(
                    target.position, "glyphs/maki.png", EffectIds.FAMILY, 10f, 1.2f);
        }
    }

    private static CharacterRuntime enemy(SupportAbilityContext context) {
        return context.battle.targetOf(context.team.main().playerId);
    }

    private static List<CharacterRuntime> enemies(SupportAbilityContext context) {
        return context.battle.players().stream()
                .filter(target -> target.playerId != context.team.main().playerId
                        && target.isAlive())
                .toList();
    }
}
