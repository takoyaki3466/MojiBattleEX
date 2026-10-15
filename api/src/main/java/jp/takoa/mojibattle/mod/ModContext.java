package jp.takoa.mojibattle.mod;

import jp.takoa.mojibattle.content.ContentRegistrar;
import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.resource.ResourceManager;

/** MOD IDと登録購読境界を結び付ける不変コンテキスト。 */
public record ModContext(String modId, RegistrationBus registrationBus, ResourceManager resources,
                         ContentRegistrar content) {
    public ModContext {
        if (modId == null || modId.isBlank() || registrationBus == null || resources == null || content == null) {
            throw new IllegalArgumentException("Mod context fields are required");
        }
    }
}
