package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.audio.BattleSoundCue;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.domain.combat.UltimateStrike;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusStackRule;
import jp.takoa.mojibattle.domain.status.StatusType;
import jp.takoa.mojibattle.registry.EffectIds;

/** 設定された投射物群または自己強化を生成するデータ駆動型メインスキル。 */
public final class StockRiseSkill implements ISkill {
    private static final float GLOBAL_SKILL_DAMAGE_SCALE = .55f;
    private static final int BUFF_PROJECTILE_COUNT = 5;
    private static final float BUFF_PROJECTILE_SPREAD_DEGREES = 5f;

    private final AbilityConfigs.ProjectileSkill config;

    public StockRiseSkill(AbilityConfigs.ProjectileSkill config) {
        this.config = config;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public void initialize(CharacterRuntime user, float cooldownMultiplier) {
        if (config.maximumStocks > 1) {
            user.configureSkillStocks(
                    config.maximumStocks,
                    config.initialStocks,
                    config.stockRechargeSeconds * cooldownMultiplier);
        }
    }

    @Override
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || !user.skillReady()) {
            return false;
        }
        float healthCost = user.maxHp() * Math.max(0, config.healthCostRatio);
        if (healthCost > 0 && !user.spendHealth(healthCost)) {
            return false;
        }
        if (config.maximumStocks > 1 && !user.consumeSkillStock()) {
            return false;
        }
        battle.emitSound(BattleSoundCue.SKILL);
        if ("self_attack_buff".equals(config.type)) {
            activateSelfAttackBuff(battle, user);
            return true;
        }
        if ("random_economy".equals(config.type)) {
            activateRandomEconomy(battle, user);
            return true;
        }
        fire(battle, user, true, false);
        return true;
    }

    /** 自己強化を付与し、現在の対象方向へ演出用の寿司投射物を扇状に生成する。 */
    private void activateSelfAttackBuff(BattleController battle, CharacterRuntime user) {
        user.startSkillCooldown(battle.skillCooldown(user, config.cooldownSeconds));
        battle.addSkillUltimatePoints(user, config.ultimatePointsPerHit);
        user.statuses.apply(new StatusEffect(
                config.id,
                user.playerId,
                StatusType.ATTACK_UP,
                config.buffAttackMultiplier,
                config.buffDurationSeconds));

        CharacterRuntime target = battle.targetOf(user.playerId);
        Vector3 direction = target == null
                ? new Vector3(user.facing)
                : new Vector3(target.position).sub(user.position);
        direction.y = 0f;
        if (direction.isZero()) {
            direction.set(user.facing);
        }
        direction.nor();
        for (int shot = 0; shot < BUFF_PROJECTILE_COUNT; shot++) {
            Vector3 aimed = new Vector3(direction)
                    .rotate(Vector3.Y, (shot - BUFF_PROJECTILE_COUNT / 2) * BUFF_PROJECTILE_SPREAD_DEGREES);
            Vector3 start = new Vector3(user.position).mulAdd(aimed, 1.1f).add(0, 1.05f, 0);
            battle.spawn(new Projectile(
                    battle.nextAttackId(),
                    user.playerId,
                    "寿司",
                    config.textures[0],
                    config.projectileEffectId,
                    start,
                    aimed.scl(config.projectileSpeed),
                    config.projectileRadius,
                    0,
                    0,
                    config.projectileLifetimeSeconds));
            battle.emitImageEffect(
                    start,
                    config.textures[0],
                    config.activationEffectId,
                    4.5f + shot * .35f,
                    .55f);
        }
    }

    /** 自身と各敵へ、好況または不況の攻防補正をそれぞれ50%で付与する。 */
    private void activateRandomEconomy(BattleController battle, CharacterRuntime user) {
        user.startSkillCooldown(battle.skillCooldown(user, config.cooldownSeconds));
        battle.addSkillUltimatePoints(user, config.ultimatePointsPerHit);
        applyEconomyRoll(battle, user, user);
        for (CharacterRuntime target : battle.players()) {
            if (target.playerId != user.playerId && target.isAlive()) {
                applyEconomyRoll(battle, user, target);
            }
        }
    }

    private void applyEconomyRoll(
            BattleController battle,
            CharacterRuntime source,
            CharacterRuntime target) {
        boolean boom = MathUtils.randomBoolean();
        target.statuses.apply(new StatusEffect(
                config.id + ":attack",
                source.playerId,
                boom ? StatusType.ATTACK_UP : StatusType.ATTACK_DOWN,
                boom ? config.buffAttackMultiplier : config.debuffAttackMultiplier,
                config.buffDurationSeconds,
                StatusStackRule.REPLACE,
                1));
        target.statuses.apply(new StatusEffect(
                config.id + ":defense",
                source.playerId,
                boom ? StatusType.DAMAGE_TAKEN_DOWN : StatusType.DAMAGE_TAKEN_UP,
                boom ? config.buffDefenseMultiplier : config.debuffDefenseMultiplier,
                config.buffDurationSeconds,
                StatusStackRule.REPLACE,
                1));
        battle.emitImageEffect(
                target.position,
                config.textures[0],
                boom ? EffectIds.FAMILY : EffectIds.FIRE,
                boom ? 8f : 10f,
                1.1f);
    }

    @Override
    public void activateFromUltimate(BattleController battle, CharacterRuntime user) {
        fire(battle, user, false, true);
    }

    /**
     * 設定された編隊で投射物を生成する。
     *
     * @param consumeResources 通常発動として再使用時間と即時必殺ポイントを処理するか
     * @param ultimateAttack 必殺技由来の攻撃として印を付けるか
     */
    private void fire(
            BattleController battle,
            CharacterRuntime user,
            boolean consumeResources,
            boolean ultimateAttack) {
        int projectileCount = "bind_follow_up".equals(config.type) ? 1 : config.glyphs.length;
        if (consumeResources) {
            user.startSkillCooldown(battle.skillCooldown(user, config.cooldownSeconds));
            if (!config.pointsOnHit) {
                battle.addSkillUltimatePoints(user, config.ultimatePointsPerHit * projectileCount);
            }
        }
        battle.emitEffect(new Vector3(user.position).add(0, 1, 0), config.activationEffectId, 2.8f, .32f);
        if (consumeResources && config.activationTexture != null) {
            battle.emitImageEffect(
                    new Vector3(user.position).add(0, 1.4f, 0),
                    config.activationTexture,
                    config.activationEffectId,
                    config.activationEffectScale,
                    .8f);
        }
        long attackId = battle.nextAttackId();
        Vector3 forward = new Vector3(user.facing).nor();
        // 移動処理と同じ画面右方向を使い、投射物の編隊が操作方向と反転しないようにする。
        Vector3 right = new Vector3(-forward.z, 0, forward.x);
        for (int i = 0; i < projectileCount; i++) {
            float[] offset = config.offsets[i];
            Vector3 shotDirection = config.radial
                    ? new Vector3(
                            MathUtils.sin(MathUtils.PI2 * i / config.glyphs.length),
                            0,
                            MathUtils.cos(MathUtils.PI2 * i / config.glyphs.length))
                    : new Vector3(forward);
            Vector3 start = new Vector3(user.position).mulAdd(shotDirection, 1.0f);
            if (!config.radial) {
                start.mulAdd(right, offset[0]);
            }
            start.y += 1.1f + offset[1];
            float damage = config.damages != null && i < config.damages.length
                    ? config.damages[i]
                    : config.baseDamage;
            float bind = config.bindDurations != null && i < config.bindDurations.length
                    ? config.bindDurations[i]
                    : config.bindSeconds;
            Projectile projectile = new Projectile(
                    attackId,
                    user.playerId,
                    config.glyphs[i],
                    config.textures[i],
                    config.projectileEffectId,
                    start,
                    shotDirection.scl(config.projectileSpeed * 1.3f),
                    config.projectileRadius,
                    damage * GLOBAL_SKILL_DAMAGE_SCALE,
                    config.stunSeconds,
                    bind,
                    config.projectileLifetimeSeconds,
                    config.boomerang,
                    config.boomerangDistance,
                    config.pointsOnHit ? config.ultimatePointsPerHit : 0);
            projectile.attackTraits(ultimateAttack, false).source(user);
            if ("bind_follow_up".equals(config.type)) {
                projectile.followUp(
                        config.glyphs[1],
                        config.followUpTexture,
                        config.followUpDamage * GLOBAL_SKILL_DAMAGE_SCALE,
                        config.followUpRadius,
                        config.followUpSpeed);
            }
            battle.spawn(projectile);
        }
        if (consumeResources && config.fallingObjectTexture != null) {
            spawnFallingObject(battle, user);
        }
    }

    /** 投射編隊に加え、対象位置へ通常スキル扱いの落下物を生成する。 */
    private void spawnFallingObject(BattleController battle, CharacterRuntime user) {
        CharacterRuntime target = battle.targetOf(user.playerId);
        if (target == null) {
            return;
        }
        battle.spawn(new UltimateStrike(
                user.playerId,
                target.position,
                config.fallingRadius,
                config.fallingDamage * GLOBAL_SKILL_DAMAGE_SCALE,
                config.fallingWarningSeconds,
                config.fallingEffectSeconds,
                config.fallingSpawnHeight,
                config.activationEffectId,
                config.hitEffectId,
                "building",
                config.fallingObjectTexture,
                config.fallingHorizontalOffset).skillAttack());
    }
}
