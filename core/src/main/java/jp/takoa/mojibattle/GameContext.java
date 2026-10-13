package jp.takoa.mojibattle;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.custom.CustomCharacterStore;
import jp.takoa.mojibattle.domain.CharacterRegistry;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.support.SupportRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.ConfiguredUltimateFactory;
import jp.takoa.mojibattle.domain.ability.LongTermInvestmentPassive;
import jp.takoa.mojibattle.domain.ability.StockRiseSkill;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.log.GameLog;
import jp.takoa.mojibattle.ui.UiFactory;

/**
 * 画面間で共有する設定、レジストリ、ローカライズおよびUIリソースを構築して保持する。
 *
 * <p>コンストラクタは設定の読込み後に標準キャラクターと保存済みカスタムキャラクターを登録し、
 * その表示文字を収録した {@link Skin} を生成する。生成したSkinの所有権はこのクラスにあり、
 * アプリケーション終了時に {@link #close()} を呼び出す必要がある。</p>
 */
public final class GameContext implements AutoCloseable {
    public final ConfigManager config;
    public final LocalizationManager i18n;
    public final CharacterRegistry characters;
    public final SupportRegistry supports;
    public final AbilityRegistry abilities;
    public final CustomCharacterStore customCharacters;
    public final Skin skin;

    /** 共有サービスを設定データから構築し、相互参照可能な状態へ初期化する。 */
    public GameContext() {
        config = new ConfigManager();
        GameLog.info("Config", "Configuration loaded and validated");
        i18n = new LocalizationManager(config.game.language);
        characters = new CharacterRegistry();
        supports = new SupportRegistry();
        abilities = new AbilityRegistry();
        customCharacters = new CustomCharacterStore(config);
        StringBuilder extraCharacters = new StringBuilder();
        for (var character : config.characters()) {
            extraCharacters.append(character.displayGlyph);
        }
        for (var support : config.supports()) {
            extraCharacters.append(support.displayGlyph);
        }
        for (var custom : customCharacters.all()) {
            if (custom.displayName != null) {
                extraCharacters.append(custom.displayName);
            }
        }
        for (var skill : config.skills()) {
            extraCharacters.append(String.join("", skill.glyphs));
        }
        skin = UiFactory.createSkin(i18n, extraCharacters.toString());
        for (var character : config.characters()) {
            characters.register(character);
        }
        for (var support : config.supports()) {
            supports.register(support);
        }
        for (var custom : customCharacters.all()) {
            if (custom.type == CharacterType.MAIN) {
                characters.register(customCharacters.createMain(custom));
            } else {
                supports.register(customCharacters.createSupport(custom));
            }
        }
        for (var skill : config.skills()) {
            abilities.register(new StockRiseSkill(skill));
        }
        for (var ultimate : config.ultimates()) {
            abilities.register(ConfiguredUltimateFactory.create(ultimate));
        }
        for (var passive : config.passives()) {
            abilities.register(new LongTermInvestmentPassive(passive));
        }
        GameLog.info("Registry", "Registered characters: " + config.characters().size());
        validateTranslations();
    }

    /** 設定から参照される必須翻訳キーがすべて存在することを検証する。 */
    private void validateTranslations() {
        for (var character : config.characters()) {
            if (!i18n.contains(character.nameKey)) {
                throw new IllegalArgumentException("Missing i18n key: " + character.nameKey);
            }
            var ultimate = config.ultimates().stream()
                    .filter(value -> value.id.equals(character.ultimateId))
                    .findFirst()
                    .orElseThrow();
            if (!i18n.contains(ultimate.nameKey)) {
                throw new IllegalArgumentException("Missing i18n key: " + ultimate.nameKey);
            }
            String[] requiredKeys = {
                    "ability." + character.id + ".skill.name",
                    "ability." + character.id + ".passive.name"
            };
            for (String key : requiredKeys) {
                requireTranslation(key);
            }
        }
        for (var support : config.supports()) {
            String[] requiredKeys = {support.nameKey, support.skill.nameKey, support.ultimate.nameKey};
            for (String key : requiredKeys) {
                requireTranslation(key);
            }
        }
    }

    /**
     * 翻訳キーが存在しない場合に、設定不備を示す例外を送出する。
     *
     * @param key 検証する翻訳キー
     */
    private void requireTranslation(String key) {
        if (!i18n.contains(key)) {
            throw new IllegalArgumentException("Missing i18n key: " + key);
        }
    }

    /** このコンテキストが所有するUIリソースを解放する。 */
    @Override
    public void close() {
        skin.dispose();
    }
}
