package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigInt;
import jp.takoa.mojibattle.ability.config.annotations.ConfigResource;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.context.PassiveUpdateContext;
import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 標準パッシブを登録IDごとの具象Runtimeとして提供する。 */
public final class StandardPassives {
    private StandardPassives() {
    }

    /** スキル成立ごとに次回必殺技の追加攻撃数を増加させる。 */
    public static final class CompanyGrowth extends AbstractRuntime<CompanyGrowth.Config> implements PassiveRuntime {
        private long extraUltimateAttacks;

        public CompanyGrowth(Config config) {
            super(config);
        }

        @Override
        public void onSkillActivated(AbilityActivationContext context) {
            extraUltimateAttacks = extraUltimateAttacks > Long.MAX_VALUE - 2
                    ? Long.MAX_VALUE : extraUltimateAttacks + 2;
        }

        @Override
        public long additionalUltimateAttackCount(CombatantView owner) {
            return extraUltimateAttacks;
        }

        public record Config() {
        }
    }

    /** 攻撃判定の倍率を変更する。 */
    public static final class HitboxBoost extends AbstractRuntime<HitboxBoost.Config> implements PassiveRuntime {
        public HitboxBoost(Config config) {
            super(config);
        }

        @Override
        public float hitboxMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "hitboxMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** 経過時間に応じて必殺技ポイントを加算する。 */
    public static final class UltimateRegen extends AbstractRuntime<UltimateRegen.Config> implements PassiveRuntime {
        public UltimateRegen(Config config) {
            super(config);
        }

        @Override
        public void update(PassiveUpdateContext context) {
            context.actions().addUltimatePoints(context.owner(), getConfig().pointsPerSecond() * context.deltaSeconds());
        }

        public record Config(
                @ConfigFloat(name = "ultimatePointsPerSecond", defaultValue = 0f) float pointsPerSecond) {
        }
    }

    /** 被ダメージ倍率を変更する。 */
    public static final class DamageGuard extends AbstractRuntime<DamageGuard.Config> implements PassiveRuntime {
        public DamageGuard(Config config) {
            super(config);
        }

        @Override
        public float incomingDamageMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "incomingDamageMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** 移動速度倍率を変更する。 */
    public static final class SpeedBoost extends AbstractRuntime<SpeedBoost.Config> implements PassiveRuntime {
        public SpeedBoost(Config config) {
            super(config);
        }

        @Override
        public float moveSpeedMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "moveSpeedMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** 必殺技ポイント獲得倍率を変更する。 */
    public static final class UltimateGain extends AbstractRuntime<UltimateGain.Config> implements PassiveRuntime {
        public UltimateGain(Config config) {
            super(config);
        }

        @Override
        public float ultimateGainMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "ultimateGainMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** スタン時間倍率を変更する。 */
    public static final class StunBoost extends AbstractRuntime<StunBoost.Config> implements PassiveRuntime {
        public StunBoost(Config config) {
            super(config);
        }

        @Override
        public float stunMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "stunMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** スキルクールダウン倍率を変更する。 */
    public static final class SkillHaste extends AbstractRuntime<SkillHaste.Config> implements PassiveRuntime {
        public SkillHaste(Config config) {
            super(config);
        }

        @Override
        public float skillCooldownMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "skillCooldownMultiplier", defaultValue = 1f, exclusiveMin = true) float multiplier) {
        }
    }

    /** 生存中の味方数に応じて攻撃倍率を増加させる。 */
    public static final class FamilyBond extends AbstractRuntime<FamilyBond.Config> implements PassiveRuntime {
        private int allies = 1;

        public FamilyBond(Config config) {
            super(config);
        }

        @Override
        public void update(PassiveUpdateContext context) {
            allies = Math.max(1, context.actions().battlefield().alliesOf(context.owner()).size());
        }

        @Override
        public float attackMultiplier(CombatantView owner) {
            float multiplier = 1f + Math.max(0, allies - 1) * getConfig().bonusPerMember();
            return Math.min(getConfig().maximumMultiplier(), multiplier);
        }

        public record Config(
                @ConfigFloat(name = "familyBonusPerMember", defaultValue = 0f) float bonusPerMember,
                @ConfigFloat(defaultValue = 1f, min = 1f) float maximumMultiplier) {
        }
    }

    /** 固定周期の決定的抽選で敵をスタンさせる。 */
    public static final class DifficultKanji extends AbstractRuntime<DifficultKanji.Config> implements PassiveRuntime {
        private float intervalTimer;
        private float procCooldown;

        public DifficultKanji(Config config) {
            super(config);
        }

        @Override
        public void update(PassiveUpdateContext context) {
            Config config = getConfig();
            procCooldown = Math.max(0f, procCooldown - context.deltaSeconds());
            intervalTimer -= context.deltaSeconds();
            if (intervalTimer > 0f) {
                return;
            }
            intervalTimer += config.intervalSeconds();
            long interval = (long) Math.floor(context.battleElapsedSeconds() / config.intervalSeconds());
            long roll = Math.floorMod(interval * 3701L + context.owner().instanceId().hashCode() * 1709L, 10000L);
            if (procCooldown <= 0f && roll < config.chance() * 10000f) {
                List<CombatantView> enemies = context.actions().enemiesOf(context.owner());
                if (!enemies.isEmpty()) {
                    context.actions().stun(enemies.getFirst(), config.stunSeconds());
                }
                procCooldown = config.internalCooldown();
            }
        }

        public record Config(
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float intervalSeconds,
                @ConfigFloat(name = "procChance", defaultValue = 0f, max = 1f) float chance,
                @ConfigFloat(name = "procStunSeconds", defaultValue = .1f, exclusiveMin = true) float stunSeconds,
                @ConfigFloat(name = "procInternalCooldown", defaultValue = 0f) float internalCooldown) {
        }
    }

    /** 減少HP率に応じて攻撃倍率を増加させる。 */
    public static final class Berserker extends AbstractRuntime<Berserker.Config> implements PassiveRuntime {
        public Berserker(Config config) {
            super(config);
        }

        @Override
        public float attackMultiplier(CombatantView owner) {
            float missingRatio = 1f - owner.hp() / owner.maximumHp();
            return Math.min(getConfig().maximumMultiplier(), 1f + missingRatio * getConfig().bonus());
        }

        public record Config(
                @ConfigFloat(name = "missingHealthAttackBonus", defaultValue = 1f, exclusiveMin = true) float bonus,
                @ConfigFloat(defaultValue = 1f, min = 1f) float maximumMultiplier) {
        }
    }

    /** 必殺技ダメージ倍率を変更する。 */
    public static final class UltimateBoost extends AbstractRuntime<UltimateBoost.Config> implements PassiveRuntime {
        public UltimateBoost(Config config) {
            super(config);
        }

        @Override
        public float ultimateDamageMultiplier(CombatantView owner) {
            return getConfig().multiplier();
        }

        public record Config(
                @ConfigFloat(name = "ultimateDamageMultiplier", defaultValue = 1f, min = 1f) float multiplier) {
        }
    }

    /** 固定周期で爆破可能なマーカーを生成する。 */
    public static final class CashScatter extends AbstractRuntime<CashScatter.Config> implements PassiveRuntime {
        private float intervalTimer;

        public CashScatter(Config config) {
            super(config);
        }

        @Override
        public void update(PassiveUpdateContext context) {
            intervalTimer -= context.deltaSeconds();
            if (intervalTimer > 0f) {
                return;
            }
            intervalTimer += getConfig().intervalSeconds();
            context.actions().spawnDetonatableMarker(context.owner(), getConfig().texture(), getConfig().maximumObjects());
        }

        public record Config(
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float intervalSeconds,
                @RequiredConfig @ConfigResource(name = "spawnObjectTexture", defaultValue = "mojibattle:objects/cash-bundle.png") ResourceId texture,
                @ConfigInt(name = "maximumSpawnedObjects", defaultValue = 1, min = 1) int maximumObjects) {
        }
    }
}
