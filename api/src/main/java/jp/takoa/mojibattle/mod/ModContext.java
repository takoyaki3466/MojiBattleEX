package jp.takoa.mojibattle.mod;

import jp.takoa.mojibattle.ability.AbilityImplementationRegistrar;
import jp.takoa.mojibattle.content.ContentRegistrar;
import jp.takoa.mojibattle.event.RegistrationBus;
import jp.takoa.mojibattle.resource.ResourceManager;

/** MOD IDと登録購読境界を結び付ける不変コンテキスト。 */
public record ModContext(String modId, RegistrationBus registrationBus, ResourceManager resources,
                         ContentRegistrar content, AbilityImplementationRegistrar abilities) {
    public ModContext {
        if (modId == null || modId.isBlank() || registrationBus == null || resources == null || content == null || abilities == null) {
            throw new IllegalArgumentException("Mod context fields are required");
        }
    }

    /** API 1.xで公開済みのコンストラクタとのバイナリ互換用。 */
    @Deprecated
    public ModContext(String modId, RegistrationBus registrationBus, ResourceManager resources,
                      ContentRegistrar content) {
        this(modId, registrationBus, resources, content, UnsupportedAbilities.INSTANCE);
    }

    private enum UnsupportedAbilities implements AbilityImplementationRegistrar {
        INSTANCE;

        private UnsupportedOperationException unavailable() {
            return new UnsupportedOperationException("This host does not expose external Ability registration");
        }

        @Override
        public void registerSkillImplementation(String id, jp.takoa.mojibattle.ability.AbilityFactory<? extends jp.takoa.mojibattle.ability.SkillRuntime> factory) {
            throw unavailable();
        }

        @Override
        public void registerUltimateImplementation(String id, jp.takoa.mojibattle.ability.AbilityFactory<? extends jp.takoa.mojibattle.ability.UltimateRuntime> factory) {
            throw unavailable();
        }

        @Override
        public void registerPassiveImplementation(String id, jp.takoa.mojibattle.ability.AbilityFactory<? extends jp.takoa.mojibattle.ability.PassiveRuntime> factory) {
            throw unavailable();
        }
    }
}
