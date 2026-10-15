package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.content.ContentRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** MODごとの公開登録を検証し、データロード開始まで保持する起動時コレクター。 */
final class ContentRegistrationCollector {
    private final List<CharacterDataRegistration> characters = new ArrayList<>();
    private final Set<String> characterResources = new HashSet<>();
    private boolean sealed;

    ContentRegistrar forMod(String modId) {
        return registration -> {
            if (sealed) {
                throw new IllegalStateException("Content registration is already sealed");
            }
            if (registration == null || !modId.equals(registration.namespace())) {
                throw new IllegalArgumentException("MOD may register only its own resource namespace: " + modId);
            }
            String key = registration.character().toString();
            if (!characterResources.add(key)) {
                throw new IllegalArgumentException("Duplicate character data registration: " + key);
            }
            characters.add(registration);
        };
    }

    List<CharacterDataRegistration> seal() {
        sealed = true;
        return List.copyOf(characters);
    }
}
