package jp.takoa.mojibattle.battle.presentation;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 必殺技などが要求するカットイン画像、見出し、カメラ演出の不変仕様。
 *
 * <h2>関係</h2>
 * <p>画像を{@link ResourceId}、カメラ意図を{@link CutInMotion}で表す。
 * Abilityが構築して{@link jp.takoa.mojibattle.battle.action.CombatActions#showCutIn}へ渡し、
 * ホストのカットインキュー、HUD、カメラ制御から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.showCutIn(user, new CutInSpec(image,
 *         "example.ultimate.title", 1.5f, 100, CutInMotion.ORBIT_RUSH, 1.1f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>描画設定が無効な場合も戦闘ロジックを変えず、同時発動時の並べ替え・分割表示をホスト側で一貫して処理するために作成された。</p>
 *
 * @param image               カットイン画像ID
 * @param titleTranslationKey 表示見出しの翻訳キー
 * @param durationSeconds     表示を維持する正の秒数
 * @param priority            同時表示時の優先順位
 * @param motion              カメラワークの意味種別
 * @param intensity           カメラおよび表示演出の正の強度
 */
public record CutInSpec(ResourceId image, String titleTranslationKey, float durationSeconds, int priority, CutInMotion motion, float intensity) {
    /**
     * 既存能力向けに標準の旋回カメラを適用する。
     */
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
