package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.content.ContentRegistrar;
import jp.takoa.mojibattle.content.SupportCharacterDataRegistration;
import jp.takoa.mojibattle.content.FieldEffectDataRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** MODごとの公開登録を検証し、データロード開始まで保持する起動時コレクター。 */
final class ContentRegistrationCollector {
    private final List<CharacterDataRegistration> characters = new ArrayList<>();
    private final Set<String> characterResources = new HashSet<>();
    private final List<SupportCharacterDataRegistration> supports = new ArrayList<>();
    private final List<FieldEffectDataRegistration> fieldEffects = new ArrayList<>();
    private final Set<String> supportResources = new HashSet<>();
    private final Set<String> fieldResources = new HashSet<>();
    private boolean sealed;

    ContentRegistrar forMod(String modId) {
        return new ContentRegistrar() {
            @Override
            public void registerCharacter(CharacterDataRegistration registration) {
                requireOpenOwned(modId, registration == null ? null : registration.namespace());
                String key = registration.character().toString();
                if (!characterResources.add(key)) {
                    throw new IllegalArgumentException("Duplicate character data registration: " + key);
                }
                characters.add(registration);
            }

            @Override
            public void registerSupportCharacter(SupportCharacterDataRegistration registration) {
                requireOpenOwned(modId, registration == null ? null : registration.namespace());
                String key = registration.support().toString();
                if (!supportResources.add(key)) {
                    throw new IllegalArgumentException("Duplicate support data registration: " + key);
                }
                supports.add(registration);
            }

            @Override
            public void registerFieldEffect(FieldEffectDataRegistration registration) {
                requireOpenOwned(modId, registration == null ? null : registration.namespace());
                String key = registration.fieldEffect().toString();
                if (!fieldResources.add(key)) {
                    throw new IllegalArgumentException("Duplicate field effect registration: " + key);
                }
                fieldEffects.add(registration);
            }
        };
    }

    Snapshot seal() {
        sealed = true;
        return new Snapshot(List.copyOf(characters), List.copyOf(supports), List.copyOf(fieldEffects));
    }

    private void requireOpenOwned(String modId, String namespace) {
        if (sealed) {
            throw new IllegalStateException("Content registration is already sealed");
        }
        if (!modId.equals(namespace)) {
            throw new IllegalArgumentException("MOD may register only its own resource namespace: " + modId);
        }
    }

    record Snapshot(List<CharacterDataRegistration> characters,
                    List<SupportCharacterDataRegistration> supports,
                    List<FieldEffectDataRegistration> fieldEffects) {
    }
}
