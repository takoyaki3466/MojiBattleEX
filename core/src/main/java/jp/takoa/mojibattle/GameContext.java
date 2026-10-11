package jp.takoa.mojibattle;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.domain.CharacterRegistry;
import jp.takoa.mojibattle.domain.SupportRegistry;
import jp.takoa.mojibattle.domain.ability.*;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.ui.UiFactory;
import jp.takoa.mojibattle.log.GameLog;

public final class GameContext implements AutoCloseable {
    public final ConfigManager config;
    public final LocalizationManager i18n;
    public final CharacterRegistry characters;
    public final SupportRegistry supports;
    public final AbilityRegistry abilities;
    public final Skin skin;

    public GameContext() {
        config = new ConfigManager();
        GameLog.info("Config", "Configuration loaded and validated");
        i18n = new LocalizationManager(config.game.language);
        characters = new CharacterRegistry();
        supports = new SupportRegistry();
        abilities = new AbilityRegistry();
        StringBuilder extraCharacters = new StringBuilder();
        for (var character : config.characters()) extraCharacters.append(character.displayGlyph);
        for(var support:config.supports())extraCharacters.append(support.displayGlyph);
        for (var skill : config.skills()) extraCharacters.append(String.join("", skill.glyphs));
        skin = UiFactory.createSkin(i18n, extraCharacters.toString());
        for (var character : config.characters()) characters.register(character);
        for(var support:config.supports())supports.register(support);
        for (var skill : config.skills()) abilities.register(new StockRiseSkill(skill));
        for (var ultimate : config.ultimates()) abilities.register(ConfiguredUltimateFactory.create(ultimate));
        for (var passive : config.passives()) abilities.register(new LongTermInvestmentPassive(passive));
        GameLog.info("Registry", "Registered characters: " + config.characters().size());
        validateTranslations();
    }
    private void validateTranslations() {
        for (var character : config.characters()) {
            if (!i18n.contains(character.nameKey)) throw new IllegalArgumentException("Missing i18n key: " + character.nameKey);
            var ultimate = config.ultimates().stream().filter(value -> value.id.equals(character.ultimateId)).findFirst().orElseThrow();
            if (!i18n.contains(ultimate.nameKey)) throw new IllegalArgumentException("Missing i18n key: " + ultimate.nameKey);
            for (String key : new String[]{"ability." + character.id + ".skill.name", "ability." + character.id + ".passive.name"})
                if (!i18n.contains(key)) throw new IllegalArgumentException("Missing i18n key: " + key);
        }
        for(var support:config.supports()){
            for(String key:new String[]{support.nameKey,support.skill.nameKey,support.ultimate.nameKey})
                if(!i18n.contains(key))throw new IllegalArgumentException("Missing i18n key: "+key);
        }
    }
    @Override public void close() { skin.dispose(); }
}
