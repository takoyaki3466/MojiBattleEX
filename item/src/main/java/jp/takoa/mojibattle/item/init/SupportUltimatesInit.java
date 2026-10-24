package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.NewSupportAbilities;
import jp.takoa.mojibattle.item.ability.StandardSupportAbilities;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/**
 * 補助キャラクターの必殺技だけを登録する。
 */
public final class SupportUltimatesInit {
    public static final MojiBattleRegister.SupportAbility REGISTER = MojiBattleRegister.createSupportAbility(ItemContentModule.MOD_ID);

    static {
        StandardSupportAbilities.registerUltimates(REGISTER);
        NewSupportAbilities.registerUltimates(REGISTER);
    }

    private SupportUltimatesInit() {
    }
}
