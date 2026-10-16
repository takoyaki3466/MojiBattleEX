package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.AbilityImplementationRegistrar;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.ability.SupportPassiveContext;
import jp.takoa.mojibattle.ability.SupportPassiveRuntime;
import jp.takoa.mojibattle.battle.AreaAttackSpec;
import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.DamageRequest;
import jp.takoa.mojibattle.battle.DamageType;
import jp.takoa.mojibattle.battle.DomainSpec;
import jp.takoa.mojibattle.battle.DomainVisualStyle;
import jp.takoa.mojibattle.battle.StatusConsumption;
import jp.takoa.mojibattle.battle.StatusDuration;
import jp.takoa.mojibattle.battle.StatusEffectType;
import jp.takoa.mojibattle.battle.StatusSpec;
import jp.takoa.mojibattle.battle.StatusStacking;
import jp.takoa.mojibattle.battle.SupportNormalAttackModifier;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/** 新規補助2体の、公開APIだけに依存する能力実装。 */
public final class NewSupportAbilities {
    private static final ResourceId BLOOM = id("everbloom_garden");
    private static final ResourceId FLOWER_ATTACK = id("flower_petals");

    private NewSupportAbilities() {
    }

    public static void register(AbilityImplementationRegistrar registrar) {
        registrar.registerSupportSkillImplementation("mojibattle:southern_mark", config -> context -> {
            for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
                context.actions().applyStatus(context.owner(), enemy, new StatusSpec(
                        id("southern_mark_status"), StatusEffectType.NEXT_INCOMING_DAMAGE_MULTIPLIER,
                        1.65f, StatusDuration.seconds(8f), StatusStacking.REFRESH, 1, 1f,
                        StatusConsumption.ON_DAMAGE_RECEIVED));
            }
            context.actions().emitEffect(context.owner(), id("wind"), 10f, .75f);
            return true;
        });
        registrar.registerSupportUltimateImplementation("mojibattle:southern_inversion", config -> context -> {
            for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
                context.actions().mirrorAcrossArenaCenter(enemy);
                context.actions().damage(context.owner(), enemy, DamageRequest.of(82f, DamageType.ULTIMATE));
            }
            return true;
        });
        registrar.registerSupportPassiveImplementation("mojibattle:southern_cross_passive", config -> new SouthernCross());

        registrar.registerSupportSkillImplementation("mojibattle:petal_rebirth", config -> context -> {
            for (CombatantView ally : context.actions().alliesOf(context.owner())) {
                context.actions().applyStatus(context.owner(), ally, new StatusSpec(
                        id("petal_rebirth_hot"), StatusEffectType.HEAL_OVER_TIME, 15f,
                        StatusDuration.seconds(7f), StatusStacking.ADD_STACK, 3, 1f));
            }
            return true;
        });
        registrar.registerSupportUltimateImplementation("mojibattle:everbloom", config -> context -> {
            context.actions().activateDomain(context.owner(), new DomainSpec(
                    BLOOM, 14f, DomainVisualStyle.BLOOM, .9f, id("star")));
            for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
                context.actions().spawnAreaAttack(context.owner(), enemy, new AreaAttackSpec(
                        .45f, 6f, DamageRequest.of(74f, DamageType.ULTIMATE),
                        id("star"), id("star"), id("glyphs/flower.png"), 10f));
            }
            return true;
        });
        registrar.registerSupportPassiveImplementation("mojibattle:flowering_cycle", config -> new FloweringCycle());
    }

    private static final class SouthernCross implements SupportPassiveRuntime {
        @Override
        public void onBattleStart(SupportPassiveContext context) {
            context.supportActions().addNormalAttackModifier(new SupportNormalAttackModifier(
                    id("southern_cross_shot"), 60, SupportNormalAttackModifier.Stacking.ADD,
                    0f, 0f, 1, 0, 42f, id("glyphs/south.png"), id("wind")));
            context.supportActions().setStateLines(List.of(
                    AbilityStateLine.of("state.south.cross", 60)));
        }
    }

    /** 花園領域中だけ五方向の花弁弾へ変化し、領域終了時に確実に解除する。 */
    private static final class FloweringCycle implements SupportPassiveRuntime {
        private boolean active;

        @Override
        public void update(SupportPassiveContext context, float deltaSeconds) {
            boolean next = context.actions().isDomainActive(context.owner(), BLOOM);
            if (next && !active) {
                context.supportActions().addNormalAttackModifier(new SupportNormalAttackModifier(
                        FLOWER_ATTACK, 80, SupportNormalAttackModifier.Stacking.ADD,
                        -.15f, -.35f, 4, 0, 120f, id("glyphs/flower.png"), id("star")));
            } else if (!next && active) {
                context.supportActions().removeNormalAttackModifier(FLOWER_ATTACK);
            }
            active = next;
            context.supportActions().setStateLines(List.of(
                    AbilityStateLine.of("state.flower.garden", 80, active ? "ON" : "OFF")));
        }
    }

    private static ResourceId id(String path) {
        return new ResourceId("mojibattle", path);
    }
}
