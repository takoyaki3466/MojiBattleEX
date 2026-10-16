package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/** 必殺技などが要求する全画面カットインとカメラ演出の定義。 */
public record CutInSpec(ResourceId image, String titleTranslationKey, float durationSeconds,
                        int priority, CutInMotion motion, float intensity) {
    /** 既存能力向けに標準の旋回カメラを適用する。 */
    public CutInSpec(ResourceId image, String titleTranslationKey, float durationSeconds, int priority) {
        this(image, titleTranslationKey, durationSeconds, priority, CutInMotion.ORBIT_RUSH, 1f);
    }

    public CutInSpec {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(motion, "motion");
        if (titleTranslationKey == null || titleTranslationKey.isBlank()) {
            throw new IllegalArgumentException("Cut-in title translation key is required");
        }
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Cut-in duration must be positive and finite");
        }
        if (!Float.isFinite(intensity) || intensity <= 0) {
            throw new IllegalArgumentException("Cut-in intensity must be positive and finite");
        }
    }
}
