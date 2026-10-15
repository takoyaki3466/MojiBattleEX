package jp.takoa.mojibattle.item.impl.registry;

import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.item.impl.ability.LongTermInvestmentPassive;
import jp.takoa.mojibattle.registry.AbilityPresentation;
import jp.takoa.mojibattle.registry.PassiveImplementation;

import java.util.function.Consumer;

/**
 * パッシブの共有Factoryと、実装IDごとの固有検証を実装側に保持する。
 */
public final class PassiveImplementations {
    private PassiveImplementations() {
    }

    public static PassiveImplementation standard(String id) {
        return create(id, ignored -> {
        });
    }

    public static PassiveImplementation difficultKanji() {
        return create("difficult_kanji", c -> require(c.procChance >= 0 && c.procChance <= 1 && c.procStunSeconds > 0 && c.procInternalCooldown >= 0, "Difficult-kanji passive config is invalid: " + c.id));
    }

    public static PassiveImplementation berserker() {
        return create("berserker", c -> require(c.missingHealthAttackBonus > 0, "Berserker passive config is invalid: " + c.id));
    }

    public static PassiveImplementation ultimateBoost() {
        return create("ultimate_boost", c -> require(c.ultimateDamageMultiplier >= 1, "Ultimate-boost passive config is invalid: " + c.id));
    }

    public static PassiveImplementation cashScatter() {
        return create("cash_scatter", c -> require(c.spawnObjectTexture != null && c.maximumSpawnedObjects > 0, "Cash-scatter passive config is invalid: " + c.id));
    }

    private static PassiveImplementation create(String id, Consumer<AbilityConfigs.TimedPassive> validator) {
        return new PassiveImplementation(id, LongTermInvestmentPassive::new, validator, config -> AbilityPresentation.of(config.descriptionKey));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
