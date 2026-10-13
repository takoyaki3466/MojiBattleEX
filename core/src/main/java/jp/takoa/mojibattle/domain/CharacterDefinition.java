package jp.takoa.mojibattle.domain;

/**
 * メインキャラクターの基礎能力、外観および装備する能力IDを保持する定義DTO。
 *
 * <p>組込みキャラクターとカスタムキャラクターで同じ実行時生成経路を利用するため、
 * カスタム固有の保存枠情報も保持する。</p>
 */
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
    /** カスタムキャラクターが装備する複数のパッシブID。組込みキャラクターは {@link #passiveId} を使用する。 */
    public String[] passiveIds;
    public String glyphTexture;
    public String displayName;
    public String appearanceId;
    public boolean custom;
    public boolean trainingDummy;
    public int customSlot;
    public float criticalRate;
    public float criticalDamageMultiplier;
    public boolean criticalEnabled;
    /** 防御系倍率と覚醒防御を適用せず、基礎防御0として扱う場合は {@code true}。 */
    public boolean zeroDefense;

    /**
     * 実際に装備するパッシブIDを、組込み・カスタムの差を吸収して返す。
     *
     * @return 装備中のパッシブID。未設定の場合は空配列
     */
    public String[] equippedPassiveIds() {
        if (passiveIds != null && passiveIds.length > 0) {
            return passiveIds;
        }
        return passiveId == null ? new String[0] : new String[]{passiveId};
    }
}
