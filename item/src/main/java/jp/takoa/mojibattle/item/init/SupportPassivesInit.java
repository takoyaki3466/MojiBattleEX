package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.item.ItemContentModule;
import jp.takoa.mojibattle.item.ability.NewSupportAbilities;
import jp.takoa.mojibattle.registry.MojiBattleRegister;

/**
 * 補助キャラクターのパッシブだけを登録する。
 */
public final class SupportPassivesInit {
    public static final MojiBattleRegister.SupportPassive REGISTER = MojiBattleRegister.createSupportPassive(ItemContentModule.MOD_ID);

    static {
        NewSupportAbilities.registerPassives(REGISTER);
    }

    private SupportPassivesInit() {
    }
}
