package jp.takoa.mojibattle.domain;

public final class CharacterDefinition {
    public CharacterType type = CharacterType.MAIN;
    public String id;
    public String displayGlyph;
    public String uiLabel;
    public String nameKey;
    public float maxHp;
    public float baseAttack;
    public float moveSpeed;
    public float collisionRadius;
    public String skillId;
    public String ultimateId;
    public String passiveId;
    public String glyphTexture;
    public float criticalRate, criticalDamageMultiplier;
    public boolean criticalEnabled;
}
