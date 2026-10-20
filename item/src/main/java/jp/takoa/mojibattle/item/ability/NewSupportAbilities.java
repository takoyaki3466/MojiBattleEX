package jp.takoa.mojibattle.item.ability;

import jp.takoa.mojibattle.ability.presentation.AbilityStateLine;
import jp.takoa.mojibattle.ability.context.SupportPassiveContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime;
import jp.takoa.mojibattle.battle.action.spec.AreaAttackSpec;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.battle.presentation.DomainSpec;
import jp.takoa.mojibattle.battle.presentation.DomainVisualStyle;
import jp.takoa.mojibattle.battle.modifier.SupportNormalAttackModifier;
import jp.takoa.mojibattle.resource.ResourceId;
import jp.takoa.mojibattle.registry.MojiBattleRegister;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.registry.impl.SupportPassiveImpl;

import java.util.List;
import java.util.function.Supplier;

/** 新規補助2体の、公開APIだけに依存する能力実装。 */
public final class NewSupportAbilities {
    private static final ResourceId BLOOM = id("everbloom_garden");
    private static final ResourceId FLOWER_ATTACK = id("flower_petals");

    private NewSupportAbilities() {
    }

    /** 新規補助キャラクターの必殺技を登録する。 */
    public static void registerUltimates(MojiBattleRegister<SupportAbilityImpl> abilities) {
        ability(abilities, "southern_inversion", context -> {
            for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
                context.actions().mirrorAcrossArenaCenter(enemy);
                context.actions().damage(context.owner(), enemy, DamageRequest.of(82f, DamageType.ULTIMATE));
            }
            return true;
        });
        ability(abilities, "everbloom", context -> {
            context.actions().activateDomain(context.owner(), new DomainSpec(
                    BLOOM, 14f, DomainVisualStyle.BLOOM, .9f, id("star")));
            for (CombatantView enemy : context.actions().enemiesOf(context.owner())) {
                context.actions().spawnAreaAttack(context.owner(), enemy, new AreaAttackSpec(
                        .45f, 6f, DamageRequest.of(74f, DamageType.ULTIMATE),
                        id("star"), id("star"), id("glyphs/flower.png"), 10f));
            }
            return true;
        });
    }

    /** 新規補助キャラクターのパッシブを登録する。 */
    public static void registerPassives(MojiBattleRegister<SupportPassiveImpl> passives) {
        passive(passives, "southern_cross_passive", SouthernCross::new);
        passive(passives, "flowering_cycle", FloweringCycle::new);
    }

    private static void ability(MojiBattleRegister<SupportAbilityImpl> register, String id,
                                SupportAbilityRuntime runtime) {
        register.register(id, () -> new SupportAbilityImpl("mojibattle:" + id, ignored -> runtime,
                ignored -> { }));
    }

    private static void passive(MojiBattleRegister<SupportPassiveImpl> register, String id,
                                Supplier<? extends SupportPassiveRuntime> factory) {
        register.register(id, () -> new SupportPassiveImpl("mojibattle:" + id, ignored -> factory.get()));
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
