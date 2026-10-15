package jp.takoa.mojibattle.registry;

import java.util.Set;

/**
 * VFXを安定IDから解決するための描画非依存な定義。
 */
public record CombatEffectDefinition(String id, String texturePath, float scale, float duration, String animationType, float pulseSpeed, float rotationSpeed, Set<Usage> usages, int legacyIndex) {
    public CombatEffectDefinition {
        usages = Set.copyOf(usages);
    }

    /**
     * エフェクトを割り当てられる演出上の用途。
     */
    public enum Usage {
        SKILL, PROJECTILE, HIT, ULTIMATE_WARNING, ULTIMATE_IMPACT, GENERAL
    }
}
