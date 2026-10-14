package jp.takoa.mojibattle.domain.support;

/**
 * 補助キャラクターのスキルまたは必殺技を設定から構築するための定義DTO。
 */
public final class SupportAbilityDefinition {
    public String id;
    public String type;
    public String nameKey;

    /**
     * UI説明の翻訳キー。旧JSONでは読込み時に補助キャラクターIDから補完される。
     */
    public String descriptionKey;
    public float cooldownSeconds;
    public float durationSeconds;
    public float magnitude;
    public float pointCost;
    public float baseDamage;
    public float range;
}
