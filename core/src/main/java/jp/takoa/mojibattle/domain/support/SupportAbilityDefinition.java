package jp.takoa.mojibattle.domain.support;

/** 補助キャラクターのスキルまたは必殺技を設定から構築するための定義DTO。 */
public final class SupportAbilityDefinition {
    public String id;
    public String type;
    public String nameKey;
    public float cooldownSeconds;
    public float durationSeconds;
    public float magnitude;
    public float pointCost;
    public float baseDamage;
    public float range;
}
