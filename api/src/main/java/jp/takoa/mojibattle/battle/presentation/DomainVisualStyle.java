package jp.takoa.mojibattle.battle.presentation;

/**
 * 領域発動中にホストが戦場へ適用する視覚テーマ。能力値効果は含まない。
 *
 * <h2>関係</h2>
 * <p>{@link DomainSpec}が一つ保持し、ホストの戦場レンダラーから参照される。
 * Abilityはシェーダーや色定義ではなくこの意味的テーマだけに依存する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * DomainVisualStyle style = DomainVisualStyle.SUNRISE;
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>描画実装をAPIから隠し、ホストがテーマごとの表現を品質設定やプラットフォームに合わせて変更できるようにするために作成された。</p>
 */
public enum DomainVisualStyle {
    MONOCHROME,
    SUNRISE,
    BLOOM,
    ABYSS
}
