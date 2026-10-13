package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.domain.CharacterType;

/** 補助キャラクターの追従AI、通常攻撃、能力および外観を保持する定義DTO。 */
public final class SupportCharacterDefinition {
    public String id;
    public String displayGlyph;
    public String nameKey;
    public String glyphTexture;
    public String projectileTexture;
    public CharacterType type = CharacterType.SUPPORT;
    public float moveSpeed;
    public float followDistance;
    public float attackRange;
    public float attackInterval;
    public float baseDamage;
    public float projectileSpeed;
    public float ultimateChargePerSecond;
    public float criticalRate;
    public float criticalDamageMultiplier;
    public boolean criticalEnabled;
    public boolean curvedProjectile;
    public SupportAbilityDefinition skill;
    public SupportAbilityDefinition ultimate;
    public String[] fieldPassiveIds;
    public String displayName;
    public String appearanceId;
    public boolean custom;
    public int customSlot;
}
