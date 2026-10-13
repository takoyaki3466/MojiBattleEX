package jp.takoa.mojibattle.custom;

/** 保存形式のバージョンと、カスタムキャラクターの全保存枠をまとめるルートDTO。 */
public final class CustomCharacterSave {
    public int version;
    public CustomCharacterData[] slots = new CustomCharacterData[0];
}
