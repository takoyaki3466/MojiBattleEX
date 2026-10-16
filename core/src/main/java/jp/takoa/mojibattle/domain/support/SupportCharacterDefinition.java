package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.domain.CharacterType;

/**
 * 補助キャラクターの追従AI、通常攻撃、能力および外観を保持する定義DTO。
 */
public final class SupportCharacterDefinition {
    public String id;
    public String displayGlyph;
    public String nameKey;

    /**
     * 選択画面に表示する戦闘スタイルの翻訳キー。
     */
    public String styleKey;
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

    /**
     * 編成したメインキャラクターを防御力0にする永続デバフ。
     */
    public boolean zeroDefenseMain;
    public SupportAbilityDefinition skill;
    public SupportAbilityDefinition ultimate;
    public String supportPassiveImplementationId;
    public java.util.Map<String, Object> parameters = java.util.Map.of();
    public String[] fieldPassiveIds;
    public String displayName;
    public String appearanceId;
    public boolean custom;
    public int customSlot;
}
