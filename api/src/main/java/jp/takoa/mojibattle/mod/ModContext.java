package jp.takoa.mojibattle.mod;

import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.resource.ResourceManager;

/** MOD ID、登録イベント、リソースだけを公開する不変コンテキスト。 */
public record ModContext(String modId, RegistrationBus registrationBus, ResourceManager resources) {
    public ModContext {
        if (modId == null || modId.isBlank() || registrationBus == null || resources == null) {
            throw new IllegalArgumentException("Mod context fields are required");
        }
    }
}
