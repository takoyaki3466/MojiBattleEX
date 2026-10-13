package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** 能力種別を処理へ対応付け、キャラクターIDに依存せず補助能力を実行するサービス。 */
public final class SupportAbilityService {
    private record Context(
            BattleController battle,
            PlayerTeam team,
            SupportAbilityDefinition ability) {
    }

    private final Map<String, BiConsumer<Context, Boolean>> handlers = new HashMap<>();

    public SupportAbilityService() {
        handlers.put("ally_attack_up", (context, ultimate) -> applyAllyAttackUp(context));
        handlers.put("enemy_defense_down", (context, ultimate) -> applyEnemyDefenseDown(context));
        handlers.put("enemy_attack_down", (context, ultimate) -> applyEnemyAttackDown(context));
        handlers.put("heal_cleanse", (context, ultimate) -> applyHealCleanse(context));
        handlers.put("bonus_damage", (context, ultimate) -> applyBonusDamage(context));
        handlers.put("wide_slash", (context, ultimate) -> applyWideSlash(context));
        handlers.put("enemy_slow", (context, ultimate) -> applyEnemySlow(context));
        handlers.put("right_teleport", (context, ultimate) -> applyRightTeleport(context));
        handlers.put("wind_pull", (context, ultimate) -> applyWindPull(context));
        handlers.put("global_cooldown_halve", (context, ultimate) -> applyGlobalCooldownHalve(context));
    }

    private static void applyAllyAttackUp(Context context) {
        context.team.main().statuses.apply(new StatusEffect(
                context.ability.id,
                context.team.main().playerId,
                StatusType.ATTACK_UP,
                1 + context.ability.magnitude,
                context.ability.durationSeconds));
        context.battle.emitImageEffect(
                context.team.main().position,
                "glyphs/t.png",
                14,
                8,
                1.1f);
    }

    private static void applyEnemyDefenseDown(Context context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id,
                    context.team.main().playerId,
                    StatusType.DAMAGE_TAKEN_UP,
                    1 + context.ability.magnitude,
                    context.ability.durationSeconds));
            Vector3 side = new Vector3(-target.facing.z, 0, target.facing.x);
            context.battle.emitEffect(
                    new Vector3(target.position).mulAdd(side, 3),
                    14,
                    9,
                    1.5f);
            context.battle.emitEffect(
                    new Vector3(target.position).mulAdd(side, -3),
                    14,
                    9,
                    1.5f);
        }
    }

    private static void applyEnemyAttackDown(Context context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id,
                    context.team.main().playerId,
                    StatusType.ATTACK_DOWN,
                    1 - context.ability.magnitude,
                    context.ability.durationSeconds));
            context.battle.emitImageEffect(target.position, "glyphs/zero.png", 5, 7, 1.2f);
        }
    }

    private static void applyHealCleanse(Context context) {
        context.team.main().heal(
                context.team.main().definition.maxHp * context.ability.magnitude);
        context.team.main().clearNegativeStatuses();
        context.battle.emitImageEffect(
                context.team.main().position,
                "glyphs/zero.png",
                5,
                11,
                1.5f);
    }

    private static void applyBonusDamage(Context context) {
        context.team.main().statuses.apply(new StatusEffect(
                context.ability.id,
                context.team.main().playerId,
                StatusType.BONUS_DAMAGE,
                1 + context.ability.magnitude,
                context.ability.durationSeconds));
        context.battle.emitImageEffect(
                context.team.main().position,
                "glyphs/slash.png",
                12,
                9,
                1.1f);
    }

    private static void applyWideSlash(Context context) {
        context.battle.spawn(new LineStrike(
                context.team.main().playerId,
                context.team.support().position,
                new Vector3(enemy(context).position).sub(context.team.support().position),
                context.ability.range,
                3.5f,
                context.ability.baseDamage,
                .25f,
                .8f,
                3,
                12,
                "objects/energy-lance.png"));
    }

    private static void applyEnemySlow(Context context) {
        for (CharacterRuntime target : enemies(context)) {
            target.statuses.apply(new StatusEffect(
                    context.ability.id,
                    context.team.main().playerId,
                    StatusType.MOVE_DOWN,
                    1 - context.ability.magnitude,
                    context.ability.durationSeconds));
            context.battle.emitEffect(target.position, 3, 6, 1);
        }
    }

    private static void applyRightTeleport(Context context) {
        boolean teleported = context.battle.teleportRight(
                context.team.main().playerId,
                context.ability.range);
        if (teleported) {
            context.team.main().statuses.apply(new StatusEffect(
                    context.ability.id,
                    context.team.main().playerId,
                    StatusType.ATTACK_UP,
                    1 + context.ability.magnitude,
                    context.ability.durationSeconds));
        } else {
            context.team.support().addUltimatePoints(context.ability.pointCost);
        }
    }

    private static void applyWindPull(Context context) {
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
                    target.position,
                    "glyphs/matsu.png",
                    11,
                    9,
                    1.25f);
        }
    }

    private static void applyGlobalCooldownHalve(Context context) {
        context.battle.halveAllSkillCooldowns();
        for (CharacterRuntime target : context.battle.players()) {
            context.battle.emitImageEffect(
                    target.position,
                    "glyphs/matsu.png",
                    15,
                    10,
                    1.5f);
        }
    }

    /** 発動可能な場合に補助スキルの再使用時間を開始し、対応する効果を実行する。 */
    public void useSkill(BattleController battle, PlayerTeam team) {
        SupportCharacterRuntime support = team.support();
        SupportAbilityDefinition ability = support.definition.skill;
        if (!support.skillReady() || team.main().isStunned()) {
            return;
        }
        support.startSkillCooldown(battle.supportSkillCooldown(ability.cooldownSeconds));
        execute(battle, team, ability, false);
    }

    /** 必殺ポイントを消費できる場合に補助必殺を実行し、発動表示を要求する。 */
    public void useUltimate(BattleController battle, PlayerTeam team) {
        SupportAbilityDefinition ability = team.support().definition.ultimate;
        if (team.main().isStunned() || !team.support().spendUltimate(ability.pointCost)) {
            return;
        }
        execute(battle, team, ability, true);
        battle.announceUltimate(team.main().playerId, ability.nameKey);
    }

    private void execute(
            BattleController battle,
            PlayerTeam team,
            SupportAbilityDefinition ability,
            boolean ultimate) {
        BiConsumer<Context, Boolean> handler = handlers.get(ability.type);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown support ability: " + ability.type);
        }
        handler.accept(new Context(battle, team, ability), ultimate);
        for (int ring = 0; ring < (ultimate ? 4 : 2); ring++) {
            battle.emitEffect(
                    team.support().position,
                    (ultimate ? 14 : 6) + ring,
                    ultimate ? 12 - ring * 1.6f : 7 - ring,
                    ultimate ? 1.4f : .8f);
        }
    }

    private static CharacterRuntime enemy(Context context) {
        return context.battle.targetOf(context.team.main().playerId);
    }

    private static List<CharacterRuntime> enemies(Context context) {
        return context.battle.players().stream()
                .filter(target -> target.playerId != context.team.main().playerId && target.isAlive())
                .toList();
    }
}
