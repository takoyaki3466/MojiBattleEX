package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.ability.runtime.PassiveRuntime;
import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.NewCharacterAbilities;
import jp.takoa.mojibattle.item.ability.StandardPassives;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/**
 * メインキャラクターのパッシブだけを登録する。
 */
public final class PassivesInit {
    public static final MojiBattleRegister.Passive REGISTER = MojiBattleRegister.createPassive(ItemContentModule.MOD_ID);

    static {
        register("compound_interest", StandardPassives.CompanyGrowth.class);
        register("hitbox_boost", StandardPassives.HitboxBoost.class);
        register("ultimate_regen", StandardPassives.UltimateRegen.class);
        register("damage_guard", StandardPassives.DamageGuard.class);
        register("speed_boost", StandardPassives.SpeedBoost.class);
        register("ultimate_gain", StandardPassives.UltimateGain.class);
        register("stun_boost", StandardPassives.StunBoost.class);
        register("skill_haste", StandardPassives.SkillHaste.class);
        register("family_bond", StandardPassives.FamilyBond.class);
        register("difficult_kanji", StandardPassives.DifficultKanji.class);
        register("berserker", StandardPassives.Berserker.class);
        register("ultimate_boost", StandardPassives.UltimateBoost.class);
        register("cash_scatter", StandardPassives.CashScatter.class);
        NewCharacterAbilities.registerPassives(REGISTER);
    }

    private PassivesInit() {
    }

    private static <R extends PassiveRuntime> void register(String path, Class<R> runtimeType) {
        REGISTER.registerWithPresentation(path, runtimeType, AbilityRegistrationSupport::presentation);
    }
}
