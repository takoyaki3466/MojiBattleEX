package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.domain.support.SupportAbilityContext;
import jp.takoa.mojibattle.domain.support.SupportAbilityDefinition;
import jp.takoa.mojibattle.item.impl.ability.BuiltInSupportAbilities;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;

import java.util.function.BiConsumer;

/**
 * 補助能力の実行ハンドラと共通データ検証を実装側で組み立てる。
 */
public final class SupportImplementations {
    private SupportImplementations() {
    }

    public static SupportAbilityImplementation allyAttackUp() {
        return create("ally_attack_up", (context, ultimate) -> BuiltInSupportAbilities.applyAllyAttackUp(context));
    }

    public static SupportAbilityImplementation enemyDefenseDown() {
        return create("enemy_defense_down", (context, ultimate) -> BuiltInSupportAbilities.applyEnemyDefenseDown(context));
    }

    public static SupportAbilityImplementation enemyAttackDown() {
        return create("enemy_attack_down", (context, ultimate) -> BuiltInSupportAbilities.applyEnemyAttackDown(context));
    }

    public static SupportAbilityImplementation healCleanse() {
        return create("heal_cleanse", (context, ultimate) -> BuiltInSupportAbilities.applyHealCleanse(context));
    }

    public static SupportAbilityImplementation bonusDamage() {
        return create("bonus_damage", (context, ultimate) -> BuiltInSupportAbilities.applyBonusDamage(context));
    }

    public static SupportAbilityImplementation wideSlash() {
        return create("wide_slash", (context, ultimate) -> BuiltInSupportAbilities.applyWideSlash(context));
    }

    public static SupportAbilityImplementation enemySlow() {
        return create("enemy_slow", (context, ultimate) -> BuiltInSupportAbilities.applyEnemySlow(context));
    }

    public static SupportAbilityImplementation rightTeleport() {
        return create("right_teleport", (context, ultimate) -> BuiltInSupportAbilities.applyRightTeleport(context));
    }

    public static SupportAbilityImplementation windPull() {
        return create("wind_pull", (context, ultimate) -> BuiltInSupportAbilities.applyWindPull(context));
    }

    public static SupportAbilityImplementation globalCooldownHalve() {
        return create("global_cooldown_halve", (context, ultimate) -> BuiltInSupportAbilities.applyGlobalCooldownHalve(context));
    }

    public static SupportAbilityImplementation trainCharge() {
        return create("train_charge", (context, ultimate) -> BuiltInSupportAbilities.applyTrainCharge(context));
    }

    public static SupportAbilityImplementation gatherAll() {
        return create("gather_all", (context, ultimate) -> BuiltInSupportAbilities.applyGatherAll(context));
    }

    private static SupportAbilityImplementation create(String id, BiConsumer<SupportAbilityContext, Boolean> handler) {
        return new SupportAbilityImplementation(id, handler, SupportImplementations::validate);
    }

    private static void validate(SupportAbilityDefinition definition) {
        if (definition.id == null || definition.id.isBlank() || definition.nameKey == null || definition.cooldownSeconds < 0 || definition.durationSeconds < 0 || definition.pointCost < 0) {
            throw new IllegalArgumentException("Support ability config is invalid: " + definition.id);
        }
    }
}
