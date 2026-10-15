package com.example.mojibattle;

import jp.takoa.mojibattle.content.CharacterDataRegistration;
import jp.takoa.mojibattle.mod.GameMod;
import jp.takoa.mojibattle.mod.ModContext;
import jp.takoa.mojibattle.resource.ResourceId;

/** 公開APIとJSONだけで既存実装を再利用する最小MOD例。 */
public final class ExampleMod implements GameMod {
    private static final String MOD_ID = "examplemod";

    @Override
    public String id() {
        return MOD_ID;
    }

    @Override
    public void register(ModContext context) {
        context.content().registerCharacter(new CharacterDataRegistration(
                id("characters/example.json"),
                id("abilities/example_skill.json"),
                id("abilities/example_ultimate.json"),
                id("abilities/example_passive.json"),
                "i18n"));
    }

    private static ResourceId id(String path) {
        return new ResourceId(MOD_ID, path);
    }
}
