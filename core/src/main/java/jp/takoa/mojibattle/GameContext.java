package jp.takoa.mojibattle;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import jp.takoa.mojibattle.audio.BattleAudio;
import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.custom.CustomCharacterData;
import jp.takoa.mojibattle.custom.CustomCharacterStore;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportRegistry;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.i18n.LocalizationManager;
import jp.takoa.mojibattle.log.GameLog;
import jp.takoa.mojibattle.event.EventChannels;
import jp.takoa.mojibattle.registry.BuiltInRegistries;
import jp.takoa.mojibattle.registry.RegistrySnapshot;
import jp.takoa.mojibattle.ui.UiFactory;
import jp.takoa.mojibattle.resource.ResourceManager;

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
    public final BattleAudio audio;
    public final BuiltInRegistries registries;
    public final EventChannels events;
    public final ResourceManager resources;
    private final AutoCloseable modRuntime;

    /**
     * 共有サービスを設定データから構築し、相互参照可能な状態へ初期化する。
     */
    public GameContext(RegistrySnapshot bootstrap) {
        this(bootstrap, null);
    }

    /**
     * MOD ClassLoader等、ゲーム終了時に閉じる外部ランタイムを伴って共有サービスを構築する。
     */
    public GameContext(RegistrySnapshot bootstrap, AutoCloseable modRuntime) {
        this.modRuntime = modRuntime;
        registries = bootstrap.registries();
        events = bootstrap.events();
        resources = bootstrap.resources();
        config = bootstrap.config();

        GameLog.info("Config", "Configuration loaded and validated");

        audio = new BattleAudio(config.game.masterVolume);

        i18n = bootstrap.i18n();
        characters = bootstrap.characters();
        supports = bootstrap.supports();
        abilities = bootstrap.abilities();
        customCharacters = bootstrap.customCharacters();

        StringBuilder extraCharacters = new StringBuilder();

        for (CharacterDefinition character : config.characters()) {
            extraCharacters.append(character.displayGlyph);
        }

        for (SupportCharacterDefinition support : config.supports()) {
            extraCharacters.append(support.displayGlyph);
        }

        for (CustomCharacterData custom : customCharacters.all()) {
            if (custom.displayName != null) {
                extraCharacters.append(custom.displayName);
            }
        }

        for (AbilityConfigs.ProjectileSkill skill : config.skills()) {
            extraCharacters.append(String.join("", skill.glyphs));
        }

        skin = UiFactory.createSkin(i18n, extraCharacters.toString());
        GameLog.info("Registry", "Registered characters: " + config.characters().size());
    }

    /**
     * このコンテキストが所有するUIリソースを解放する。
     */
    @Override
    public void close() {
        RuntimeException failure = null;
        failure = closeStep(failure, audio::dispose);
        failure = closeStep(failure, skin::dispose);
        failure = closeStep(failure, events::close);
        if (modRuntime != null) {
            failure = closeStep(failure, modRuntime::close);
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** 一つの解放失敗で後続リソースの解放を省略せず、追加失敗をsuppressedへ保持する。 */
    private static RuntimeException closeStep(RuntimeException failure, CloseStep step) {
        try {
            step.close();
            return failure;
        } catch (Exception error) {
            RuntimeException wrapped = error instanceof RuntimeException runtime
                    ? runtime
                    : new IllegalStateException("Cannot close game resource", error);
            if (failure == null) {
                return wrapped;
            }
            failure.addSuppressed(wrapped);
            return failure;
        }
    }

    @FunctionalInterface
    private interface CloseStep {
        void close() throws Exception;
    }
}
