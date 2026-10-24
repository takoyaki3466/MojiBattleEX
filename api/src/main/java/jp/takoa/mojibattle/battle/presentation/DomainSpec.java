package jp.takoa.mojibattle.battle.presentation;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 重ねて発動できる領域の視覚定義。領域自体はキャラクターへの効果を持たない。
 *
 * <h2>関係</h2>
 * <p>安定IDと演出IDを{@link ResourceId}、テーマを{@link DomainVisualStyle}で表す。
 * Abilityが構築して{@link jp.takoa.mojibattle.battle.action.CombatActions#activateDomain}へ渡し、
 * ホストの領域寿命管理と戦場描画から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.activateDomain(user, new DomainSpec(domainId, 8f,
 *         DomainVisualStyle.ABYSS, 0.8f, ambientFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>能力値効果と視覚領域を分離し、複数領域を同時合成しても個別のRuntime状態へ依存しないようにするために作成された。</p>
 *
 * @param id              領域の安定ID
 * @param durationSeconds 有効時間
 * @param visualStyle     戦場の視覚テーマ
 * @param intensity       0から1までの演出強度
 * @param ambientEffectId 任意の継続演出ID
 */
public record DomainSpec(ResourceId id, float durationSeconds, DomainVisualStyle visualStyle, float intensity, ResourceId ambientEffectId) {
    public DomainSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(visualStyle, "visualStyle");

        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0 || !Float.isFinite(intensity) || intensity < 0 || intensity > 1) {
            throw new IllegalArgumentException("Domain duration or intensity is invalid");
        }
    }
}
