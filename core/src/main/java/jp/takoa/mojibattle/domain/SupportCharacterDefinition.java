package jp.takoa.mojibattle.domain;

public final class SupportCharacterDefinition {
    public String id, displayGlyph, nameKey, glyphTexture, projectileTexture;
    public CharacterType type = CharacterType.SUPPORT;
    public float moveSpeed, followDistance, attackRange, attackInterval, baseDamage, projectileSpeed, ultimateChargePerSecond;
    public float criticalRate, criticalDamageMultiplier;
    public boolean criticalEnabled, curvedProjectile;
    public SupportAbilityDefinition skill, ultimate;
    public String[] fieldPassiveIds;
}
