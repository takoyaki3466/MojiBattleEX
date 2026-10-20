package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.skill.character.DimensionalSlashSkill;
import jp.takoa.mojibattle.item.ability.skill.character.EastExchangeSkill;
import jp.takoa.mojibattle.item.ability.skill.character.PredatoryPounceSkill;
import jp.takoa.mojibattle.item.ability.skill.character.SidewaysRaidSkill;
import jp.takoa.mojibattle.item.ability.skill.standard.BindFollowUpSkill;
import jp.takoa.mojibattle.item.ability.skill.standard.ProjectileSkill;
import jp.takoa.mojibattle.item.ability.skill.standard.RandomEconomySkill;
import jp.takoa.mojibattle.item.ability.skill.standard.SelfAttackBuffSkill;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/** メインキャラクターのスキルだけを登録する。 */
public final class SkillsInit {
    public static final MojiBattleRegister.Skill REGISTER =
            MojiBattleRegister.createSkill(ItemContentModule.MOD_ID);

    static {
        register("projectile", ProjectileSkill.class);
        register("bind_follow_up", BindFollowUpSkill.class);
        register("self_attack_buff", SelfAttackBuffSkill.class);
        register("random_economy", RandomEconomySkill.class);
        register("dimensional_slash", DimensionalSlashSkill.class);
        register("predatory_pounce", PredatoryPounceSkill.class);
        register("east_exchange", EastExchangeSkill.class);
        register("sideways_raid", SidewaysRaidSkill.class);
    }

    private SkillsInit() {
    }

    private static <R extends SkillRuntime> void register(
            String path, Class<R> runtimeType) {
        REGISTER.registerWithPresentation(path, runtimeType,
                (config, multiplier) -> AbilityRegistrationSupport.presentation(config));
    }
}
