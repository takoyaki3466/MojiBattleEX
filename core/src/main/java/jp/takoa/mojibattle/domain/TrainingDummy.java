package jp.takoa.mojibattle.domain;

/** トレーニング専用の攻撃しない標的キャラクター定義を生成する。 */
public final class TrainingDummy {
    private TrainingDummy() {
    }

    /**
     * トレーニング標的として識別可能な最小限の定義を生成する。
     *
     * @return 新しく生成した標的定義
     */
    public static CharacterDefinition definition() {
        CharacterDefinition value = new CharacterDefinition();
        value.id = "training_dummy";
        value.type = CharacterType.MAIN;
        value.displayGlyph = "■";
        value.nameKey = "character.training_dummy.name";
        value.maxHp = 1;
        value.baseAttack = 0;
        value.moveSpeed = 1;
        value.collisionRadius = .7f;
        value.glyphTexture = "glyphs/training-dummy.png";
        value.trainingDummy = true;
        return value;
    }
}
