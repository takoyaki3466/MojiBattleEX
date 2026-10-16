package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 重ねて発動できる領域の視覚定義。領域自体はキャラクターへの効果を持たない。
 *
 * @param id 領域の安定ID
 * @param durationSeconds 有効時間
 * @param visualStyle 戦場の視覚テーマ
 * @param intensity 0から1までの演出強度
 * @param ambientEffectId 任意の継続演出ID
 */
public record DomainSpec(ResourceId id, float durationSeconds, DomainVisualStyle visualStyle,
                         float intensity, ResourceId ambientEffectId) {
    public DomainSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(visualStyle, "visualStyle");
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0
                || !Float.isFinite(intensity) || intensity < 0 || intensity > 1) {
            throw new IllegalArgumentException("Domain duration or intensity is invalid");
        }
    }
}
