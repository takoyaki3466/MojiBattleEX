package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.action.spec.LineAttackSpec;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.battle.status.StatusDuration;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.registry.MojiBattleRegister;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Locale;

/** 標準補助必殺技を登録IDごとの具象Runtimeとして提供する。 */
public final class StandardSupportAbilities {
    private StandardSupportAbilities() {
    }

    /** 標準補助必殺技を型付きRuntimeクラスとして登録する。 */
    public static void registerUltimates(MojiBattleRegister.SupportAbility abilities) {
        abilities.register("enemy_defense_down", EnemyDefenseDown.class);
        abilities.register("heal_cleanse", HealCleanse.class);
        abilities.register("wide_slash", WideSlash.class);
        abilities.register("right_teleport", RightTeleport.class);
        abilities.register("global_cooldown_halve", GlobalCooldownHalve.class);
        abilities.register("gather_all", GatherAll.class);
    }

    /** 敵が受けるダメージの倍率を一定時間増加させる。 */
    public static final class EnemyDefenseDown extends AbstractRuntime<EnemyDefenseDown.Config>
            implements SupportAbilityRuntime {
        public EnemyDefenseDown(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            applyEnemies(context, StatusEffectType.INCOMING_DAMAGE_MULTIPLIER,
                    1f + getConfig().magnitude(), getConfig().duration());
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float magnitude,
                @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
        }
    }

    /** 味方を割合回復し、弱体状態を解除する。 */
    public static final class HealCleanse extends AbstractRuntime<HealCleanse.Config>
            implements SupportAbilityRuntime {
        public HealCleanse(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            context.actions().healRatio(context.owner(), getConfig().ratio());
            context.actions().cleanseNegativeStatuses(context.owner());
            return true;
        }

        public record Config(
                @ConfigFloat(name = "magnitude", defaultValue = 0f, max = 1f) float ratio) {
        }
    }

    /** 広い直線範囲へ攻撃する。 */
    public static final class WideSlash extends AbstractRuntime<WideSlash.Config>
            implements SupportAbilityRuntime {
        public WideSlash(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            context.actions().spawnLineAttack(context.owner(), new LineAttackSpec(
                    getConfig().range(), 3.5f, DamageRequest.of(getConfig().damage(), DamageType.ULTIMATE),
                    .25f, .8f, 1, id("speed"), id("critical"), id("objects/energy-lance.png")));
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 1f, exclusiveMin = true) float range,
                @ConfigFloat(name = "baseDamage", defaultValue = 0f) float damage) {
        }
    }

    /** 対象の横へ移動し、成功時に攻撃倍率を増加させる。 */
    public static final class RightTeleport extends AbstractRuntime<RightTeleport.Config>
            implements SupportAbilityRuntime {
        public RightTeleport(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            Config config = getConfig();
            if (!context.actions().teleportBesideTarget(context.owner(), config.range())) {
                context.supportActions().refundUltimatePoints(config.pointCost());
            } else {
                apply(context, context.owner(), StatusEffectType.ATTACK_MULTIPLIER,
                        1f + config.magnitude(), config.duration());
            }
            return true;
        }

        public record Config(
                @ConfigFloat(defaultValue = 0f) float magnitude,
                @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration,
                @ConfigFloat(defaultValue = 0f) float range,
                @ConfigFloat(defaultValue = 0f) float pointCost) {
        }
    }

    /** 全スキルの残りクールダウンを半減する。 */
    public static final class GlobalCooldownHalve extends AbstractRuntime<GlobalCooldownHalve.Config>
            implements SupportAbilityRuntime {
        public GlobalCooldownHalve(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            context.actions().halveAllSkillCooldowns();
            return true;
        }

        public record Config() {
        }
    }

    /** メイン戦闘員を戦場中央へ集合させる。 */
    public static final class GatherAll extends AbstractRuntime<GatherAll.Config>
            implements SupportAbilityRuntime {
        public GatherAll(Config config) {
            super(config);
        }

        @Override
        public boolean activate(SupportAbilityActivationContext context) {
            context.actions().gatherMainCombatantsAtCenter(.12f);
            return true;
        }

        public record Config() {
        }
    }

    private static void applyEnemies(SupportAbilityActivationContext context, StatusEffectType type,
                                     float value, float seconds) {
        for (CombatantView target : context.actions().enemiesOf(context.owner())) {
            apply(context, target, type, value, seconds);
        }
    }

    private static void apply(SupportAbilityActivationContext context, CombatantView target,
                              StatusEffectType type, float value, float seconds) {
        context.actions().applyStatus(context.owner(), target, StatusSpec.multiplier(
                id("support/" + type.name().toLowerCase(Locale.ROOT)), type, Math.max(0f, value),
                StatusDuration.seconds(seconds)));
    }

    private static ResourceId id(String path) {
        return new ResourceId("mojibattle", path);
    }
}
