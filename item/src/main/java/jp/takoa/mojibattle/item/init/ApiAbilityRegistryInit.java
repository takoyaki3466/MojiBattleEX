package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.ability.AbilityImplementationRegistrar;
import jp.takoa.mojibattle.item.ability.NewCharacterAbilities;
import jp.takoa.mojibattle.item.ability.NewSupportAbilities;

/** API実装レジストリへ新規キャラクター能力を登録する初期化境界。 */
public final class ApiAbilityRegistryInit {
    private ApiAbilityRegistryInit() {
    }

    public static void register(AbilityImplementationRegistrar registrar) {
        NewCharacterAbilities.register(registrar);
        NewSupportAbilities.register(registrar);
    }
}
