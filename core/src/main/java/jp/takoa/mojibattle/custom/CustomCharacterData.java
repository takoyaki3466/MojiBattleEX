package jp.takoa.mojibattle.custom;

import jp.takoa.mojibattle.domain.CharacterType;

/**
 * カスタムキャラクター1枠分の永続化データ。
 *
 * <p>フィールド名は保存JSONのキーとして使用されるため、互換性維持のため変更しない。</p>
 */
public final class CustomCharacterData {
    public int slot;
    public int version;
    public String id;
    public CharacterType type;
    public String displayName;
    public String appearanceId;
    public String skillId;
    public String ultimateId;
    public String[] passiveIds;
    public String supportSkillId;
    public String supportUltimateId;
    public String[] fieldPassiveIds;
}
