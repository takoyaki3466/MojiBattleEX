package jp.takoa.mojibattle.item;

import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.item.init.EffectRegistryInit;
import jp.takoa.mojibattle.item.init.FieldEffectRegistryInit;
import jp.takoa.mojibattle.item.init.PassivesInit;
import jp.takoa.mojibattle.item.init.SkillsInit;
import jp.takoa.mojibattle.item.init.SupportPassivesInit;
import jp.takoa.mojibattle.item.init.SupportSkillsInit;
import jp.takoa.mojibattle.item.init.SupportUltimatesInit;
import jp.takoa.mojibattle.item.init.UltimatesInit;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.mod.ModContext;
import jp.takoa.mojibattle.mod.ModEntry;

/**
 * 組み込みコンテンツMODの入口。Bootstrapから個別実装を隠し、各DeferredRegisterを
 * 登録ライフサイクルバスへ接続することだけを担当する。
 */
@ModEntry(ItemContentModule.MOD_ID)
public final class ItemContentModule implements GameMod {
    public static final String MOD_ID = "mojibattle";

    @Override
    public void register(ModContext context) {
        attach(context.registrationBus());
    }

    /**
     * 旧Bootstrapからの呼出しを維持する互換入口。
     * 新しいホストは {@link #register(ModContext)} を使用する。
     */
    @Deprecated
    public void attach(RegistrationBus lifecycleBus) {
        EffectRegistryInit.REGISTER.attach(lifecycleBus);
        SkillsInit.REGISTER.attach(lifecycleBus);
        UltimatesInit.REGISTER.attach(lifecycleBus);
        PassivesInit.REGISTER.attach(lifecycleBus);
        FieldEffectRegistryInit.REGISTER.attach(lifecycleBus);
        SupportSkillsInit.REGISTER.attach(lifecycleBus);
        SupportUltimatesInit.REGISTER.attach(lifecycleBus);
        SupportPassivesInit.REGISTER.attach(lifecycleBus);
    }
}
