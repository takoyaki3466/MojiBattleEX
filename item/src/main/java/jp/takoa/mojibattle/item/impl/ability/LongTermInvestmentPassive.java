package jp.takoa.mojibattle.item.impl.ability;

import jp.takoa.mojibattle.domain.ability.*;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.combat.CashBundle;
import jp.takoa.mojibattle.registry.EffectIds;

import java.util.EnumMap;

/** 設定種別に応じた各種倍率と継続効果を提供するデータ駆動パッシブ。 */
public final class LongTermInvestmentPassive implements IPassive {
    private final AbilityConfigs.TimedPassive config;
    private final EnumMap<PlayerId, Float> rollTimers = new EnumMap<>(PlayerId.class);
    private final EnumMap<PlayerId, Float> cooldowns = new EnumMap<>(PlayerId.class);

    public LongTermInvestmentPassive(AbilityConfigs.TimedPassive config) {
        this.config = config;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public float attackMultiplier(CharacterRuntime owner, float elapsed, int alliedSummons) {
        if ("berserker".equals(config.type)) {
            return Math.min(
                    config.maximumMultiplier,
                    1f + (1f - owner.hpRatio()) * config.missingHealthAttackBonus);
        }
        int intervals = (int) Math.floor(Math.max(0, elapsed) / config.intervalSeconds);
        return Math.min(config.maximumMultiplier, 1f + intervals * config.attackIncreasePerInterval
                + alliedSummons * config.familyBonusPerMember);
    }

    @Override
    public float moveSpeedMultiplier(CharacterRuntime owner, float elapsed) {
        return positiveOrOne(config.moveSpeedMultiplier);
    }

    @Override
    public float skillCooldownMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.skillCooldownMultiplier);
    }

    @Override
    public float ultimateGainMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.ultimateGainMultiplier);
    }

    @Override
    public float incomingDamageMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.incomingDamageMultiplier);
    }

    @Override
    public float hitboxMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.hitboxMultiplier);
    }

    @Override
    public float stunMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.stunMultiplier);
    }

    @Override
    public float ultimateDamageMultiplier(CharacterRuntime owner) {
        return positiveOrOne(config.ultimateDamageMultiplier);
    }

    @Override
    public void update(BattleController battle, CharacterRuntime owner, float delta) {
        if (config.ultimatePointsPerSecond > 0) {
            battle.addSkillUltimatePoints(
                    owner,
                    config.ultimatePointsPerSecond * Math.max(0, delta));
        }
        if ("cash_scatter".equals(config.type)) {
            updateCashScatter(battle, owner, delta);
            return;
        }
        if (!"difficult_kanji".equals(config.type)) {
            return;
        }
        float cooldown = Math.max(0, cooldowns.getOrDefault(owner.playerId, 0f) - delta);
        cooldowns.put(owner.playerId, cooldown);
        float timer = rollTimers.getOrDefault(owner.playerId, config.intervalSeconds) - delta;
        if (timer > 0) {
            rollTimers.put(owner.playerId, timer);
            return;
        }
        rollTimers.put(owner.playerId, timer + config.intervalSeconds);
        long interval = (long) Math.floor(battle.timer().elapsed() / config.intervalSeconds);
        long roll = Math.floorMod(interval * 3701 + owner.playerId.ordinal() * 1709, 10000);
        if (cooldown <= 0 && roll < config.procChance * 10000f) {
            CharacterRuntime target = battle.targetOf(owner.playerId);
            if (target != null) {
                target.applyStun(config.procStunSeconds);
            }
            cooldowns.put(owner.playerId, config.procInternalCooldown);
        }
    }

    private void updateCashScatter(BattleController battle, CharacterRuntime owner, float delta) {
        float timer = rollTimers.getOrDefault(owner.playerId, config.intervalSeconds) - delta;
        if (timer > 0) {
            rollTimers.put(owner.playerId, timer);
            return;
        }
        rollTimers.put(owner.playerId, timer + config.intervalSeconds);
        long currentCount = battle.objects().stream()
                .filter(object -> object instanceof CashBundle cash && cash.owner == owner.playerId)
                .count();
        if (currentCount >= Math.max(1, config.maximumSpawnedObjects)) {
            return;
        }
        long interval = (long) Math.floor(battle.timer().elapsed() / config.intervalSeconds);
        float angle = Math.floorMod(interval * 137L + owner.playerId.ordinal() * 71L, 360L);
        float radius = 3f + Math.floorMod(interval, 4L) * 1.3f;
        Vector3 position = new Vector3(owner.position).add(
                MathUtils.cosDeg(angle) * radius,
                0,
                MathUtils.sinDeg(angle) * radius);
        float edge = battle.arenaHalfExtent() - .8f;
        position.x = MathUtils.clamp(position.x, -edge, edge);
        position.z = MathUtils.clamp(position.z, -edge, edge);
        battle.spawn(new CashBundle(owner.playerId, position, config.spawnObjectTexture));
        battle.emitImageEffect(position, config.spawnObjectTexture, EffectIds.CASH, 4.5f, .6f);
    }

    private static float positiveOrOne(float value) {
        return value > 0 ? value : 1f;
    }
}
