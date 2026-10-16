package jp.takoa.mojibattle.bootstrap;

import jp.takoa.mojibattle.ability.AbilityFactory;
import jp.takoa.mojibattle.ability.AbilityImplementationRegistrar;
import jp.takoa.mojibattle.ability.PassiveRuntime;
import jp.takoa.mojibattle.ability.SkillRuntime;
import jp.takoa.mojibattle.ability.UltimateRuntime;
import jp.takoa.mojibattle.ability.SupportSkillRuntime;
import jp.takoa.mojibattle.ability.SupportUltimateRuntime;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalPassiveAdapter;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalSkillAdapter;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalUltimateAdapter;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalSupportAbilityContexts;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;
import jp.takoa.mojibattle.registry.AbilityPresentation;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.PassiveImplementation;
import jp.takoa.mojibattle.registry.RegistryId;
import jp.takoa.mojibattle.registry.SkillImplementation;
import jp.takoa.mojibattle.registry.UltimateImplementation;

import java.util.LinkedHashMap;
import java.util.Map;

/** MOD別の公開Ability登録を収集し、core内部レジストリへアダプトする。 */
final class ExternalAbilityRegistrationCollector {
    private final Map<String, AbilityFactory<? extends SkillRuntime>> skills = new LinkedHashMap<>();
    private final Map<String, AbilityFactory<? extends UltimateRuntime>> ultimates = new LinkedHashMap<>();
    private final Map<String, AbilityFactory<? extends PassiveRuntime>> passives = new LinkedHashMap<>();
    private final Map<String, AbilityFactory<? extends SupportSkillRuntime>> supportSkills = new LinkedHashMap<>();
    private final Map<String, AbilityFactory<? extends SupportUltimateRuntime>> supportUltimates = new LinkedHashMap<>();
    private boolean sealed;

    AbilityImplementationRegistrar forMod(String modId) {
        return new AbilityImplementationRegistrar() {
            @Override
            public void registerSkillImplementation(String id, AbilityFactory<? extends SkillRuntime> factory) {
                register(skills, modId, id, factory, "skill");
            }

            @Override
            public void registerUltimateImplementation(String id, AbilityFactory<? extends UltimateRuntime> factory) {
                register(ultimates, modId, id, factory, "ultimate");
            }

            @Override
            public void registerPassiveImplementation(String id, AbilityFactory<? extends PassiveRuntime> factory) {
                register(passives, modId, id, factory, "passive");
            }

            @Override
            public void registerSupportSkillImplementation(String id, AbilityFactory<? extends SupportSkillRuntime> factory) {
                register(supportSkills, modId, id, factory, "support skill");
            }

            @Override
            public void registerSupportUltimateImplementation(String id, AbilityFactory<? extends SupportUltimateRuntime> factory) {
                register(supportUltimates, modId, id, factory, "support ultimate");
            }
        };
    }

    void install(BuiltInRegistries registries) {
        sealed = true;
        skills.forEach((id, factory) -> registries.skillImplementations.register(new SkillImplementation(
                id,
                config -> new ExternalSkillAdapter(config, require(factory.create(MapConfigView.of(config.parameters)), id)),
                config -> requireCommonSkill(config.cooldownSeconds, config.id),
                (config, multiplier) -> AbilityPresentation.of(config.descriptionKey))));
        ultimates.forEach((id, factory) -> registries.ultimateImplementations.register(new UltimateImplementation(
                id,
                config -> new ExternalUltimateAdapter(config, require(factory.create(MapConfigView.of(config.parameters)), id)),
                config -> requireCommonUltimate(config.pointCost, config.id),
                config -> AbilityPresentation.of(config.descriptionKey))));
        passives.forEach((id, factory) -> registries.passiveImplementations.register(new PassiveImplementation(
                id,
                config -> new ExternalPassiveAdapter(config.id, require(factory.create(MapConfigView.of(config.parameters)), id)),
                ignored -> { },
                config -> AbilityPresentation.of(config.descriptionKey))));
        supportSkills.forEach((id, factory) -> {
            Map<SupportCharacterRuntime, SupportSkillRuntime> runtimes = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
            registries.supportAbilityImplementations.register(new SupportAbilityImplementation(id,
                    (context, ignored) -> runtimes.computeIfAbsent(context.support,
                                    support -> require(factory.create(MapConfigView.of(context.ability.parameters)), id))
                            .activate(ExternalSupportAbilityContexts.create(context)),
                    ExternalAbilityRegistrationCollector::validateSupport));
        });
        supportUltimates.forEach((id, factory) -> {
            Map<SupportCharacterRuntime, SupportUltimateRuntime> runtimes = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
            registries.supportAbilityImplementations.register(new SupportAbilityImplementation(id,
                    (context, ignored) -> runtimes.computeIfAbsent(context.support,
                                    support -> require(factory.create(MapConfigView.of(context.ability.parameters)), id))
                            .activate(ExternalSupportAbilityContexts.create(context)),
                    ExternalAbilityRegistrationCollector::validateSupport));
        });
    }

    private <T> void register(Map<String, AbilityFactory<? extends T>> target, String modId, String rawId,
                              AbilityFactory<? extends T> factory, String kind) {
        if (sealed) {
            throw new IllegalStateException("Ability registration is already sealed");
        }
        if (factory == null) {
            throw new IllegalArgumentException(kind + " factory is required");
        }
        RegistryId id = RegistryId.parse(rawId);
        if (!rawId.contains(":") || !modId.equals(id.namespace())) {
            throw new IllegalArgumentException("MOD may register only namespaced " + kind + " implementations owned by " + modId);
        }
        if (target.putIfAbsent(id.toString(), factory) != null) {
            throw new IllegalArgumentException("Duplicate " + kind + " implementation: " + id);
        }
    }

    private static <T> T require(T runtime, String id) {
        if (runtime == null) {
            throw new IllegalArgumentException("Ability factory returned null: " + id);
        }
        return runtime;
    }

    private static void requireCommonSkill(float cooldown, String id) {
        if (!Float.isFinite(cooldown) || cooldown < 0) {
            throw new IllegalArgumentException("External skill cooldown is invalid: " + id);
        }
    }

    private static void requireCommonUltimate(float cost, String id) {
        if (!Float.isFinite(cost) || cost <= 0) {
            throw new IllegalArgumentException("External ultimate point cost is invalid: " + id);
        }
    }

    private static void validateSupport(jp.takoa.mojibattle.domain.support.SupportAbilityDefinition definition) {
        if (definition.id == null || definition.id.isBlank() || definition.nameKey == null
                || !Float.isFinite(definition.cooldownSeconds) || definition.cooldownSeconds < 0
                || !Float.isFinite(definition.pointCost) || definition.pointCost < 0) {
            throw new IllegalArgumentException("External support ability config is invalid: " + definition.id);
        }
    }
}
