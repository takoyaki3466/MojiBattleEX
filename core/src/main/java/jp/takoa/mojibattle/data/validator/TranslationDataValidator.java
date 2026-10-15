package jp.takoa.mojibattle.data.validator;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.i18n.LocalizationManager;

/**
 * JSON定義から参照される翻訳キーがロード済み辞書に存在することを一括検証する。
 */
public final class TranslationDataValidator {
    /**
     * キャラクター選択画面で表示される名前・能力・スタイルの翻訳キーを検証する。
     *
     * @param config 検証対象のゲームデータ
     * @param i18n ロード済みの翻訳辞書
     * @throws IllegalArgumentException 必須キーが欠落している場合
     */
    public void validate(ConfigManager config, LocalizationManager i18n) {
        for (var character : config.characters()) {
            require(i18n, character.nameKey);
            require(i18n, config.skill(character.skillId).nameKey);
            require(i18n, config.skill(character.skillId).descriptionKey);
            require(i18n, config.ultimate(character.ultimateId).nameKey);
            require(i18n, config.ultimate(character.ultimateId).descriptionKey);
            require(i18n, config.passive(character.passiveId).nameKey);
            require(i18n, config.passive(character.passiveId).descriptionKey);
        }
        for (var support : config.supports()) {
            require(i18n, support.nameKey);
            require(i18n, support.skill.nameKey);
            require(i18n, support.ultimate.nameKey);
            require(i18n, support.skill.descriptionKey);
            require(i18n, support.ultimate.descriptionKey);
            require(i18n, support.styleKey);
        }
    }

    private static void require(LocalizationManager i18n, String key) {
        if (key == null || !i18n.contains(key)) {
            throw new IllegalArgumentException("Missing i18n key: " + key);
        }
    }
}
