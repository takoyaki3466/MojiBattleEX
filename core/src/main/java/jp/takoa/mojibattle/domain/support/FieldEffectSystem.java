package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.config.FieldEffectDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * フィールド効果をID単位で重複排除し、経過時間に応じた倍率と加算値を副作用なく算出する。
 */
public final class FieldEffectSystem {
    private static final String ATTACK_GROWTH_TYPE = "attack_growth";
    private final Map<String, FieldEffectDefinition> active = new LinkedHashMap<>();

    public void activate(FieldEffectDefinition effect) {
        active.putIfAbsent(effect.id, effect);
    }

    public Collection<FieldEffectDefinition> active() {
        return active.values();
    }

    public boolean hasType(String type) {
        return active.values().stream().anyMatch(effect -> effect.type.equals(type));
    }

    /** 指定種別の効果を、時間成長の上限を考慮して乗算する。 */
    public float multiplier(String type, float elapsed) {
        float result = 1f;
        for (FieldEffectDefinition effect : active.values()) {
            if (effect.type.equals(type)) {
                float value = effect.magnitude;
                if (effect.intervalSeconds > 0) {
                    value = Math.min(
                            effect.maximum,
                            (float) Math.floor(elapsed / effect.intervalSeconds) * effect.magnitude);
                }
                result *= ATTACK_GROWTH_TYPE.equals(type) ? 1f + value : value;
            }
        }
        return result;
    }

    public float additive(String type) {
        float result = 0;
        for (FieldEffectDefinition effect : active.values()) {
            if (effect.type.equals(type)) {
                result += effect.magnitude;
            }
        }
        return result;
    }

    /** 周期内の有効時間に入っている効果だけを乗算する。 */
    public float timedMultiplier(String type, float elapsed) {
        float result = 1f;
        for (FieldEffectDefinition effect : active.values()) {
            if (effect.type.equals(type)) {
                boolean activeNow = effect.intervalSeconds <= 0
                        || effect.durationSeconds <= 0
                        || elapsed % effect.intervalSeconds < effect.durationSeconds;
                if (activeNow) {
                    result *= effect.magnitude;
                }
            }
        }
        return result;
    }

    public float interval(String type, float fallback) {
        for (FieldEffectDefinition effect : active.values()) {
            if (effect.type.equals(type) && effect.intervalSeconds > 0) {
                return effect.intervalSeconds;
            }
        }
        return fallback;
    }

    public List<String> ids() {
        return List.copyOf(active.keySet());
    }
}
