package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigBoolean;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.config.annotations.ConfigInt;
import jp.takoa.mojibattle.ability.config.annotations.ConfigResource;
import jp.takoa.mojibattle.ability.config.annotations.ConfigString;
import jp.takoa.mojibattle.ability.config.annotations.RequiredConfig;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.UltimateRuntime;
import jp.takoa.mojibattle.battle.action.spec.CompanionSummonSpec;
import jp.takoa.mojibattle.battle.action.spec.DetonationSpec;
import jp.takoa.mojibattle.battle.action.spec.FallingStrikeSpec;
import jp.takoa.mojibattle.battle.action.spec.LineAttackSpec;
import jp.takoa.mojibattle.battle.action.spec.MovingHazardSpec;
import jp.takoa.mojibattle.battle.action.spec.ProjectileBarrageSpec;
import jp.takoa.mojibattle.battle.action.spec.SkillBarrageSpec;
import jp.takoa.mojibattle.battle.action.spec.SpinningAttackSpec;
import jp.takoa.mojibattle.battle.combatant.CombatantPosition;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.concurrent.ThreadLocalRandom;

/** 標準必殺技を登録IDごとの具象Runtimeとして提供する。 */
public final class StandardUltimates {
    private StandardUltimates() {
    }

    /** 対象地点へ単発の落下攻撃を生成する。 */
    public static final class Area extends AbstractRuntime<Area.Config> implements UltimateRuntime {
        public Area(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            if (context.target() == null) {
                return false;
            }
            context.actions().spawnFallingStrike(context.user(), falling(
                    getConfig().fallingConfig(), context.target().position(), damage(getConfig()), 0f));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float radius,
                @ConfigFloat(defaultValue = 0f) float warningSeconds,
                @ConfigFloat(defaultValue = 0f) float effectSeconds,
                @ConfigFloat(defaultValue = 0f) float spawnHeight,
                @ConfigFloat(defaultValue = 0f) float fallHorizontalOffset,
                @RequiredConfig @ConfigString(defaultValue = "default") String dropStyle,
                @ConfigResource(name = "warningEffectId", defaultValue = "", nullable = true) ResourceId warningEffect,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigFloat(defaultValue = 0f) float burnDuration,
                @ConfigFloat(defaultValue = 0f) float burnDamage,
                @ConfigFloat(defaultValue = 0f) float burnTickInterval,
                @ConfigFloat(defaultValue = 0f) float burnUltimatePointsPerTick
        ) implements DamageConfig {
            public Config {
                if (burnDuration > 0f && burnTickInterval <= 0f) {
                    throw new IllegalArgumentException("burnTickInterval must be positive while burn is enabled");
                }
            }

            private FallingConfig fallingConfig() {
                return new FallingConfig(radius, warningSeconds, effectSeconds, spawnHeight, warningEffect,
                        impactEffect, dropStyle, objectTexture, fallHorizontalOffset, burnDuration, burnDamage,
                        burnTickInterval <= 0f ? 1f : burnTickInterval, burnUltimatePointsPerTick);
            }
        }
    }

    /** 自身を固定量回復する。 */
    public static final class SelfHeal extends AbstractRuntime<SelfHeal.Config> implements UltimateRuntime {
        public SelfHeal(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            context.actions().heal(context.user(), getConfig().healAmount());
            return true;
        }

        public record Config(@ConfigFloat(defaultValue = 1f, exclusiveMin = true) float healAmount) {
        }
    }

    /** 指定長・幅・方向数の直線攻撃を生成する。 */
    public static final class Line extends AbstractRuntime<Line.Config> implements UltimateRuntime {
        public Line(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().spawnLineAttack(context.user(), new LineAttackSpec(
                    config.lineLength(), config.lineWidth(), damage(config), config.warningSeconds(),
                    config.effectSeconds(), config.directions(), config.warningEffect(), config.impactEffect(),
                    config.objectTexture()));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float lineLength,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float lineWidth,
                @ConfigFloat(defaultValue = 0f) float warningSeconds,
                @ConfigFloat(defaultValue = 0f) float effectSeconds,
                @ConfigInt(defaultValue = 1, min = 1) int directions,
                @ConfigResource(name = "warningEffectId", defaultValue = "", nullable = true) ResourceId warningEffect,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture
        ) implements DamageConfig {
        }
    }

    /** 対象周辺または戦場全域へ複数の落下攻撃を生成する。 */
    public static final class MeteorShower extends AbstractRuntime<MeteorShower.Config> implements UltimateRuntime {
        public MeteorShower(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            if (context.target() == null) {
                return false;
            }
            Config config = getConfig();
            long extra = Math.max(0L, context.actions().additionalUltimateAttackCount(context.user()));
            long total = Math.min(Integer.MAX_VALUE, config.meteorCount() + extra);
            float extent = context.actions().arenaHalfExtent() * .9f;
            boolean arenaWide = "meteor".equals(config.dropStyle());
            float spread = arenaWide ? extent : Math.max(1.8f, config.radius() * .65f);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int index = 0; index < total; index++) {
                CombatantPosition center = context.target().position();
                float x = arenaWide ? random.nextFloat(-extent, extent) : center.x() + random.nextFloat(-spread, spread);
                float z = arenaWide ? random.nextFloat(-extent, extent) : center.z() + random.nextFloat(-spread, spread);
                float delay = "formula".equals(config.dropStyle()) ? index * config.barrageInterval() : 0f;
                context.actions().spawnFallingStrike(context.user(), falling(config.fallingConfig(),
                        new CombatantPosition(x, center.y(), z), damage(config), delay));
            }
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigInt(defaultValue = 1, min = 1) int meteorCount,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float radius,
                @ConfigFloat(defaultValue = 0f) float warningSeconds,
                @ConfigFloat(defaultValue = 0f) float effectSeconds,
                @ConfigFloat(defaultValue = 0f) float spawnHeight,
                @ConfigFloat(defaultValue = 0f) float fallHorizontalOffset,
                @ConfigFloat(defaultValue = 0f) float barrageInterval,
                @RequiredConfig @ConfigString(defaultValue = "default") String dropStyle,
                @ConfigResource(name = "warningEffectId", defaultValue = "", nullable = true) ResourceId warningEffect,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture
        ) implements DamageConfig {
            private FallingConfig fallingConfig() {
                return new FallingConfig(radius, warningSeconds, effectSeconds, spawnHeight, warningEffect,
                        impactEffect, dropStyle, objectTexture, fallHorizontalOffset, 0f, 0f, 1f, 0f);
            }

            public Config {
                if ("formula".equals(dropStyle) && barrageInterval <= 0f) {
                    throw new IllegalArgumentException("barrageInterval must be positive for formula meteors");
                }
            }
        }
    }

    /** 味方コンパニオンを追加召喚する。 */
    public static final class SummonFamily extends AbstractRuntime<SummonFamily.Config> implements UltimateRuntime {
        public SummonFamily(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().summonCompanions(context.user(), new CompanionSummonSpec(
                    config.summonCount(), config.objectTexture(), config.summonHp(), config.summonDefense(),
                    config.summonAttackDamage(), config.summonAttackInterval(), config.summonProjectileSpeed(),
                    config.impactEffect()));
            return true;
        }

        public record Config(
                @ConfigInt(defaultValue = 1, min = 1) int summonCount,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float summonHp,
                @ConfigFloat(defaultValue = 0f) float summonDefense,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float summonAttackDamage,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float summonAttackInterval,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float summonProjectileSpeed,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect
        ) {
        }
    }

    /** 複数の移動ハザードを生成する。 */
    public static final class MovingCar extends AbstractRuntime<MovingCar.Config> implements UltimateRuntime {
        public MovingCar(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().spawnMovingHazards(context.user(), new MovingHazardSpec(
                    config.summonCount(), config.movementSpeed(), config.radius(), damage(config),
                    config.stunSeconds(), config.effectSeconds(), config.objectTexture(), config.impactEffect()));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigInt(defaultValue = 1, min = 1) int summonCount,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float movementSpeed,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float radius,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float stunSeconds,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float effectSeconds,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect
        ) implements DamageConfig {
        }
    }

    /** 回転直線攻撃とパルス回復を生成する。 */
    public static final class SpinningOne extends AbstractRuntime<SpinningOne.Config> implements UltimateRuntime {
        public SpinningOne(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().spawnSpinningAttack(context.user(), new SpinningAttackSpec(
                    config.lineLength(), config.lineWidth(), damage(config), config.healAmount(),
                    Math.max(0f, context.user().maximumHp() - context.user().hp()), config.effectSeconds(),
                    config.objectTexture(), config.impactEffect()));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float lineLength,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float lineWidth,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float healAmount,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float effectSeconds,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect
        ) implements DamageConfig {
        }
    }

    /** 指定スキルを短い間隔で連続発動する。 */
    public static final class SkillBarrage extends AbstractRuntime<SkillBarrage.Config> implements UltimateRuntime {
        public SkillBarrage(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().startSkillBarrage(context.user(), new SkillBarrageSpec(
                    config.skillId(), config.count(), config.interval()));
            return true;
        }

        public record Config(
                @RequiredConfig @ConfigString(name = "barrageSkillId", defaultValue = "skill") String skillId,
                @ConfigInt(name = "barrageCount", defaultValue = 1, min = 1) int count,
                @ConfigFloat(name = "barrageInterval", defaultValue = .1f, exclusiveMin = true) float interval
        ) {
        }
    }

    /** 寿司弾を時間差で連射する。 */
    public static final class SushiBarrage extends AbstractRuntime<SushiBarrage.Config> implements UltimateRuntime {
        public SushiBarrage(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().startProjectileBarrage(context.user(), new ProjectileBarrageSpec(
                    config.count(), config.interval(), damage(config), config.projectileSpeed(),
                    config.projectileLifetimeSeconds(), config.projectileRadius(), config.objectTexture(),
                    config.impactEffect(), "寿司", .45f));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigInt(name = "barrageCount", defaultValue = 1, min = 1) int count,
                @ConfigFloat(name = "barrageInterval", defaultValue = .1f, exclusiveMin = true) float interval,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileSpeed,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float projectileLifetimeSeconds,
                @ConfigFloat(defaultValue = .5f, exclusiveMin = true) float projectileRadius,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect
        ) implements DamageConfig {
        }
    }

    /** 所有するマーカーを爆発させ、防御低下を付与する。 */
    public static final class CashCrash extends AbstractRuntime<CashCrash.Config> implements UltimateRuntime {
        public CashCrash(Config config) {
            super(config);
        }

        @Override
        public boolean tryActivate(AbilityActivationContext context) {
            Config config = getConfig();
            context.actions().detonateOwnedMarkers(context.user(), new DetonationSpec(
                    damage(config), config.radius(), config.warningSeconds(), config.effectSeconds(),
                    config.defenseDownMultiplier(), config.defenseDownDuration(), config.objectTexture(),
                    config.warningEffect(), config.impactEffect()));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float baseDamage,
                @ConfigBoolean(defaultValue = false) boolean ignoreDefense,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float radius,
                @ConfigFloat(defaultValue = 0f) float warningSeconds,
                @ConfigFloat(defaultValue = 0f) float effectSeconds,
                @ConfigFloat(defaultValue = 1.01f, min = 1f, exclusiveMin = true) float defenseDownMultiplier,
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float defenseDownDuration,
                @ConfigResource(defaultValue = "", nullable = true) ResourceId objectTexture,
                @ConfigResource(name = "warningEffectId", defaultValue = "", nullable = true) ResourceId warningEffect,
                @ConfigResource(name = "impactEffectId", defaultValue = "", nullable = true) ResourceId impactEffect
        ) implements DamageConfig {
        }
    }

    private interface DamageConfig {
        float baseDamage();

        boolean ignoreDefense();
    }

    private record FallingConfig(float radius, float warningSeconds, float effectSeconds, float spawnHeight,
                                 ResourceId warningEffect, ResourceId impactEffect, String dropStyle,
                                 ResourceId objectTexture, float horizontalOffset, float burnDuration,
                                 float burnDamage, float burnTickInterval, float burnUltimatePointsPerTick) {
    }

    private static DamageRequest damage(DamageConfig config) {
        return new DamageRequest(config.baseDamage(), DamageType.ULTIMATE, config.ignoreDefense(), false);
    }

    private static FallingStrikeSpec falling(FallingConfig config, CombatantPosition position,
                                              DamageRequest damage, float delay) {
        return new FallingStrikeSpec(position, config.radius(), damage, config.warningSeconds(),
                config.effectSeconds(), config.spawnHeight(), config.warningEffect(), config.impactEffect(),
                config.dropStyle(), config.objectTexture(), config.horizontalOffset(), config.burnDuration(),
                config.burnDamage(), config.burnTickInterval(), config.burnUltimatePointsPerTick(),
                Math.max(0f, delay));
    }
}
