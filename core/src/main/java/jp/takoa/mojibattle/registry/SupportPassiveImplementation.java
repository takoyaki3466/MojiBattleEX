package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.ability.AbilityFactory;
import jp.takoa.mojibattle.ability.SupportPassiveRuntime;

public record SupportPassiveImplementation(String id, AbilityFactory<? extends SupportPassiveRuntime> factory) { }
