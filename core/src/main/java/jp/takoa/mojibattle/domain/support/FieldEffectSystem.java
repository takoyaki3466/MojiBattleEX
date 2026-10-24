package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.config.FieldEffectDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * フィールド効果をID単位で重複排除し、経過時間に応じた倍率と加算値を副作用なく算出する。
 */
public final class FieldEffectSystem {
    private static final String ATTACK_GROWTH_TYPE = "attack_growth";
    private final Map<String, Activation> active = new LinkedHashMap<>();

    private record Activation(FieldEffectDefinition definition, Set<PlayerId> owners) {
    }

    public void activate(FieldEffectDefinition effect, PlayerId owner) {
        Activation activation = active.computeIfAbsent(effect.id, ignored -> new Activation(effect, new LinkedHashSet<>()));
        activation.owners.add(owner);
    }

    public Collection<FieldEffectDefinition> active() {
        return active.values().stream().map(Activation::definition).toList();
    }

    public boolean hasType(String type) {
        return active.values().stream().anyMatch(value -> value.definition.type.equals(type));
    }

    /**
     * 指定種別の効果を、時間成長の上限を考慮して乗算する。
     */
    public float multiplier(String type, float elapsed) {
        float result = 1f;
        for (FieldEffectDefinition effect : active()) {
            if (effect.type.equals(type)) {
                float value = effect.magnitude;

                if (effect.intervalSeconds > 0) {
                    value = Math.min(effect.maximum, (float) Math.floor(elapsed / effect.intervalSeconds) * effect.magnitude);
                }

                result *= ATTACK_GROWTH_TYPE.equals(type) ? 1f + value : value;
            }
        }
        return result;
    }

    public float multiplier(String type, float elapsed, CharacterRuntime target) {
        float result = 1f;
        for (Activation activation : active.values()) {
            FieldEffectDefinition effect = activation.definition;
            if (effect.type.equals(type) && applies(activation, target)) {
                float value = effect.magnitude;
                if (effect.intervalSeconds > 0) {
                    value = Math.min(effect.maximum, (float) Math.floor(elapsed / effect.intervalSeconds) * effect.magnitude);
                }
                result *= ATTACK_GROWTH_TYPE.equals(type) ? 1f + value : value;
            }
        }
        return result;
    }

    public float multiplier(String type, float elapsed, SupportCharacterRuntime target) {
        float result = 1f;
        for (Activation activation : active.values()) {
            FieldEffectDefinition effect = activation.definition;
            if (effect.type.equals(type) && applies(activation, target)) {
                float value = effect.magnitude;
                if (effect.intervalSeconds > 0) {
                    value = Math.min(effect.maximum, (float) Math.floor(elapsed / effect.intervalSeconds) * effect.magnitude);
                }
                result *= ATTACK_GROWTH_TYPE.equals(type) ? 1f + value : value;
            }
        }
        return result;
    }

    public float additive(String type) {
        float result = 0;

        for (FieldEffectDefinition effect : active()) {
            if (effect.type.equals(type)) {
                result += effect.magnitude;
            }
        }
        return result;
    }

    public float additive(String type, CharacterRuntime target) {
        float result = 0f;
        for (Activation activation : active.values()) {
            if (activation.definition.type.equals(type) && applies(activation, target)) {
                result += activation.definition.magnitude;
            }
        }
        return result;
    }

    /**
     * 周期内の有効時間に入っている効果だけを乗算する。
     */
    public float timedMultiplier(String type, float elapsed) {
        float result = 1f;

        for (FieldEffectDefinition effect : active()) {
            if (effect.type.equals(type)) {
                boolean activeNow = effect.intervalSeconds <= 0 || effect.durationSeconds <= 0 || elapsed % effect.intervalSeconds < effect.durationSeconds;

                if (activeNow) {
                    result *= effect.magnitude;
                }
            }
        }
        return result;
    }

    public float interval(String type, float fallback) {
        for (FieldEffectDefinition effect : active()) {
            if (effect.type.equals(type) && effect.intervalSeconds > 0) {
                return effect.intervalSeconds;
            }
        }
        return fallback;
    }

    public List<String> ids() {
        return List.copyOf(active.keySet());
    }

    public Set<PlayerId> ownersOfType(String type) {
        LinkedHashSet<PlayerId> result = new LinkedHashSet<>();
        active.values().stream().filter(value -> value.definition.type.equals(type)).forEach(value -> result.addAll(value.owners));
        return Set.copyOf(result);
    }

    private static boolean applies(Activation activation, CharacterRuntime target) {
        FieldEffectDefinition effect = activation.definition;
        String targetRule = effect.target == null ? "ALL" : effect.target.toUpperCase(java.util.Locale.ROOT);
        boolean teamMatch = switch (targetRule) {
            case "ALL", "MAIN" -> true;
            case "ALLIES" -> activation.owners.contains(target.playerId);
            case "ENEMIES" -> !activation.owners.contains(target.playerId);
            case "SUPPORT" -> false;
            default -> throw new IllegalArgumentException("Unknown field effect target: " + effect.target);
        };
        if (!teamMatch) return false;
        if (java.util.Arrays.asList(effect.excludeCharacterIds).contains(target.definition.id)) return false;
        return effect.includeCharacterIds.length == 0 || java.util.Arrays.asList(effect.includeCharacterIds).contains(target.definition.id);
    }

    private static boolean applies(Activation activation, SupportCharacterRuntime target) {
        FieldEffectDefinition effect = activation.definition;
        String targetRule = effect.target == null ? "ALL" : effect.target.toUpperCase(java.util.Locale.ROOT);
        boolean teamMatch = switch (targetRule) {
            case "ALL", "SUPPORT" -> true;
            case "ALLIES" -> activation.owners.contains(target.owner);
            case "ENEMIES" -> !activation.owners.contains(target.owner);
            case "MAIN" -> false;
            default -> throw new IllegalArgumentException("Unknown field effect target: " + effect.target);
        };
        if (!teamMatch) return false;
        if (java.util.Arrays.asList(effect.excludeCharacterIds).contains(target.definition.id)) return false;
        return effect.includeCharacterIds.length == 0 || java.util.Arrays.asList(effect.includeCharacterIds).contains(target.definition.id);
    }
}
