package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.config.FieldEffectDefinition;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FieldEffectSystemTest {
    @Test
    void targetAndCharacterFiltersAreDataDriven() {
        FieldEffectDefinition effect = new FieldEffectDefinition();
        effect.id = "test:enemy_slow";
        effect.type = "move_multiplier";
        effect.target = "ENEMIES";
        effect.magnitude = .5f;
        effect.maximum = .5f;
        effect.includeCharacterIds = new String[]{"included"};
        effect.excludeCharacterIds = new String[]{"immune"};
        FieldEffectSystem system = new FieldEffectSystem();
        system.activate(effect, PlayerId.ONE);

        assertEquals(1f, system.multiplier("move_multiplier", 0, runtime(PlayerId.ONE, "included")));
        assertEquals(.5f, system.multiplier("move_multiplier", 0, runtime(PlayerId.TWO, "included")));
        assertEquals(1f, system.multiplier("move_multiplier", 0, runtime(PlayerId.TWO, "other")));
        assertEquals(1f, system.multiplier("move_multiplier", 0, runtime(PlayerId.TWO, "immune")));
    }

    private static CharacterRuntime runtime(PlayerId player, String id) {
        CharacterDefinition definition = new CharacterDefinition();
        definition.id = id;
        definition.maxHp = 100;
        definition.collisionRadius = .5f;
        return new CharacterRuntime(player, definition, new Vector3());
    }
}
