package jp.takoa.mojibattle.config;

/** 覚醒アイテムの出現条件、取得判定および適用する能力補正を保持する設定DTO。 */
public final class AwakeningConfig {
    public float spawnAtRemainingSeconds;
    public float attackMultiplier;
    public float defenseMultiplier;
    public float moveSpeedMultiplier;
    public float ultimateGainMultiplier;
    public float pickupRadius;
    public float preferredDistance;
    public String texturePath;
}
