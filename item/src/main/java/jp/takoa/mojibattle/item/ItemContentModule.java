package jp.takoa.mojibattle.item;

import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.mod.ModContext;
import jp.takoa.mojibattle.item.init.EffectRegistryInit;
import jp.takoa.mojibattle.item.init.FieldEffectRegistryInit;
import jp.takoa.mojibattle.item.init.PassiveRegistryInit;
import jp.takoa.mojibattle.item.init.SkillRegistryInit;
import jp.takoa.mojibattle.item.init.SupportRegistryInit;
import jp.takoa.mojibattle.item.init.UltimateRegistryInit;
import jp.takoa.mojibattle.item.init.ApiAbilityRegistryInit;

/**
 * 組み込みコンテンツMODの入口。Bootstrapから個別実装を隠し、各DeferredRegisterを
 * 登録ライフサイクルバスへ接続することだけを担当する。
 */
public final class ItemContentModule implements GameMod {
    public static final String MOD_ID = "mojibattle";

    @Override
    public String id() {
        return MOD_ID;
    }

    @Override
    public void register(ModContext context) {
        ApiAbilityRegistryInit.register(context.abilities());
        attach(context.registrationBus());
    }

    /**
     * 旧Bootstrapからの呼出しを維持する互換入口。
     * 新しいホストは {@link #register(ModContext)} を使用する。
     */
    @Deprecated
    public void attach(jp.takoa.mojibattle.event.RegistrationBus lifecycleBus) {
        EffectRegistryInit.REGISTER.attach(lifecycleBus);
        SkillRegistryInit.REGISTER.attach(lifecycleBus);
        UltimateRegistryInit.REGISTER.attach(lifecycleBus);
        PassiveRegistryInit.REGISTER.attach(lifecycleBus);
        FieldEffectRegistryInit.REGISTER.attach(lifecycleBus);
        SupportRegistryInit.REGISTER.attach(lifecycleBus);
    }
}
