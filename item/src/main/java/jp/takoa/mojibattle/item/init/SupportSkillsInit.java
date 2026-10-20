package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.support.skill.character.PetalRebirthSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.character.SouthernMarkSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.AllyAttackUpSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.BonusDamageSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.EnemyAttackDownSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.EnemySlowSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.TrainChargeSupportSkill;
import jp.takoa.mojibattle.item.ability.support.skill.standard.WindPullSupportSkill;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/** 補助キャラクターのスキルだけを登録する。 */
public final class SupportSkillsInit {
    public static final MojiBattleRegister.SupportAbility REGISTER =
            MojiBattleRegister.createSupportAbility(ItemContentModule.MOD_ID);

    static {
        REGISTER.register("ally_attack_up", AllyAttackUpSupportSkill.class);
        REGISTER.register("enemy_attack_down", EnemyAttackDownSupportSkill.class);
        REGISTER.register("bonus_damage", BonusDamageSupportSkill.class);
        REGISTER.register("enemy_slow", EnemySlowSupportSkill.class);
        REGISTER.register("wind_pull", WindPullSupportSkill.class);
        REGISTER.register("train_charge", TrainChargeSupportSkill.class);
        REGISTER.register("southern_mark", SouthernMarkSupportSkill.class);
        REGISTER.register("petal_rebirth", PetalRebirthSupportSkill.class);
    }

    private SupportSkillsInit() {
    }
}
