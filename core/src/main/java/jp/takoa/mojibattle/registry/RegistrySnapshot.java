package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.custom.CustomCharacterStore;
import jp.takoa.mojibattle.domain.CharacterRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.support.SupportRegistry;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.event.EventChannels;
import jp.takoa.mojibattle.resource.ClasspathResourceManager;
import jp.takoa.mojibattle.resource.ResourceManager;

/**
 * itemの初期化結果をcoreの実行環境へ受け渡す、変更不能な基本データ。
 */
public record RegistrySnapshot(
        MojiBattleRegistries registries, ConfigManager config, LocalizationManager i18n,
        CharacterRegistry characters, SupportRegistry supports, AbilityRegistry abilities,
        CustomCharacterStore customCharacters, EventChannels events, ResourceManager resources) {
    /**
     * 旧7引数コンストラクタを維持し、イベントチャネルだけを補完する。
     */
    @Deprecated
    public RegistrySnapshot(MojiBattleRegistries registries, ConfigManager config, LocalizationManager i18n, CharacterRegistry characters, SupportRegistry supports, AbilityRegistry abilities, CustomCharacterStore customCharacters) {
        this(registries, config, i18n, characters, supports, abilities, customCharacters, new EventChannels(), new ClasspathResourceManager(RegistrySnapshot.class.getClassLoader()));
    }

    /**
     * リソース層追加前の8引数呼出しを維持する互換コンストラクタ。
     */
    @Deprecated
    public RegistrySnapshot(MojiBattleRegistries registries, ConfigManager config, LocalizationManager i18n, CharacterRegistry characters, SupportRegistry supports, AbilityRegistry abilities, CustomCharacterStore customCharacters, EventChannels events) {
        this(registries, config, i18n, characters, supports, abilities, customCharacters, events, new ClasspathResourceManager(RegistrySnapshot.class.getClassLoader()));
    }
}
