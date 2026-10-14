package jp.takoa.mojibattle.config;

import jp.takoa.mojibattle.data.loader.JsonDataLoader;
import jp.takoa.mojibattle.data.validator.GameDataValidator;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.registry.BuiltInRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON定義を読み込み、登録済み実装Factoryへ固有検証を委譲する設定ローダー。
 */
public final class ConfigManager {
    private final JsonDataLoader loader = new JsonDataLoader();
    private final BuiltInRegistries registries;
    public final GameConfig game;
    public final BattleConfig battle;
    public final GraphicsConfig graphics;
    public final ControlConfig controls;
    public final CriticalConfig critical;
    public final AwakeningConfig awakening;
    public final CustomEditorConfig customEditor;
    private final Map<String, CharacterDefinition> characters = new LinkedHashMap<>();
    private final Map<String, SupportCharacterDefinition> supports = new LinkedHashMap<>();
    private final Map<String, FieldEffectDefinition> fieldEffects = new LinkedHashMap<>();

    /**
     * 必須JSONを宣言順に読み、旧形式を正規化してDefinition Registryへ登録した後、全体を検証する。
     * libGDX内部ファイルAPIと、実装・演出レジストリの登録完了が前提となる。
     *
     * @param registries 実装Factoryと演出定義が登録済みの組込みレジストリ
     * @throws RuntimeException ファイル欠落、構文不正、ID重複、参照不整合がある場合
     */
    public ConfigManager(BuiltInRegistries registries) {
        this.registries = registries;
        game = read("config/game.json", GameConfig.class);
        battle = read("config/battle.json", BattleConfig.class);
        graphics = read("config/graphics.json", GraphicsConfig.class);
        controls = read("config/controls.json", ControlConfig.class);
        critical = read("config/critical.json", CriticalConfig.class);
        awakening = read("config/awakening.json", AwakeningConfig.class);
        customEditor = read("config/custom_editor.json", CustomEditorConfig.class);
        require(game.characterIds != null && game.characterIds.length > 0, "game.characterIds must not be empty");
        for (String characterId : game.characterIds) {
            loadCharacter(characterId);
        }
        require(game.supportIds != null && game.supportIds.length > 0, "game.supportIds must not be empty");
        for (String id : game.supportIds) {
            SupportCharacterDefinition support = read("config/support/" + id + ".json", SupportCharacterDefinition.class);
            require(id.equals(support.id), "Support filename ID does not match: " + id);
            normalizeSupport(support);
            require(supports.putIfAbsent(id, support) == null, "Duplicate support: " + id);
        }
        require(game.fieldEffectIds != null, "game.fieldEffectIds is required");
        for (String id : game.fieldEffectIds) {
            FieldEffectDefinition effect = read("config/field_effects/" + id + ".json", FieldEffectDefinition.class);
            require(id.equals(effect.id), "Field-effect filename ID does not match: " + id);
            require(fieldEffects.putIfAbsent(id, effect) == null, "Duplicate field effect: " + id);
        }
        new GameDataValidator(registries).validate(this);
    }

    /** 旧データで省略可能だった補助キャラクターの表示キーだけを補完する。 */
    private static void normalizeSupport(SupportCharacterDefinition support) {
        if (support.styleKey == null) {
            support.styleKey = "select.support_style." + support.id;
        }
        if (support.skill != null && support.skill.descriptionKey == null) {
            support.skill.descriptionKey = "select.support_skill_detail." + support.id;
        }
        if (support.ultimate != null && support.ultimate.descriptionKey == null) {
            support.ultimate.descriptionKey = "select.support_ultimate_detail." + support.id;
        }
    }

    /** キャラクター本体と3種類のAbility JSONを一組として読み、ID整合性を確認して登録する。 */
    private void loadCharacter(String characterId) {
        CharacterDefinition character = read("config/characters/" + characterId + ".json", CharacterDefinition.class);
        AbilityConfigs.ProjectileSkill skill = read("config/abilities/" + characterId + "_skill.json", AbilityConfigs.ProjectileSkill.class);
        AbilityConfigs.AreaUltimate ultimate = read("config/abilities/" + characterId + "_ultimate.json", AbilityConfigs.AreaUltimate.class);
        AbilityConfigs.TimedPassive passive = read("config/abilities/" + characterId + "_passive.json", AbilityConfigs.TimedPassive.class);
        require(characterId.equals(character.id), "Character filename ID does not match: " + characterId);
        normalize(characterId, skill, ultimate, passive);
        character.maxHp *= battle.healthMultiplier;
        require(characters.putIfAbsent(character.id, character) == null, "Duplicate character ID: " + character.id);
        registries.skillDefinitions.register(skill);
        registries.ultimateDefinitions.register(ultimate);
        registries.passiveDefinitions.register(passive);
    }

    /**
     * 旧type・整数VFX・暗黙翻訳キーを読み込み境界で新しい安定IDへ移行する。
     */
    private void normalize(String characterId, AbilityConfigs.ProjectileSkill skill, AbilityConfigs.AreaUltimate ultimate, AbilityConfigs.TimedPassive passive) {
        if ((skill.type == null || skill.type.isBlank()) && skill.implementationId == null) {
            skill.type = "projectile";
        }
        if (skill.implementationId == null) {
            skill.implementationId = skill.type;
        }
        if (skill.type == null) {
            skill.type = skill.implementationId;
        }
        if (ultimate.implementationId == null) {
            ultimate.implementationId = ultimate.type;
        }
        if (ultimate.type == null) {
            ultimate.type = ultimate.implementationId;
        }
        if (passive.implementationId == null) {
            passive.implementationId = passive.type;
        }
        if (passive.type == null) {
            passive.type = passive.implementationId;
        }
        if (skill.nameKey == null) {
            skill.nameKey = "ability." + characterId + ".skill.name";
        }
        if (skill.descriptionKey == null) {
            skill.descriptionKey = "guide.character." + characterId;
        }
        if (passive.nameKey == null) {
            passive.nameKey = "ability." + characterId + ".passive.name";
        }
        if (passive.descriptionKey == null) {
            passive.descriptionKey = "guide.passive_effect." + characterId;
        }
        if (ultimate.descriptionKey == null) {
            ultimate.descriptionKey = "guide.character." + characterId;
        }
        if (skill.activationEffectId == null) {
            skill.activationEffectId = effectId(skill.effectIndex);
        }
        if (skill.projectileEffectId == null) {
            skill.projectileEffectId = effectId(skill.effectIndex);
        }
        if (skill.hitEffectId == null) {
            skill.hitEffectId = effectId(skill.effectIndex);
        }
        if (ultimate.warningEffectId == null) {
            ultimate.warningEffectId = effectId(ultimate.warningEffectIndex);
        }
        if (ultimate.impactEffectId == null) {
            ultimate.impactEffectId = effectId(ultimate.impactEffectIndex);
        }
        if (ultimate.renderEffectId == null) {
            ultimate.renderEffectId = ultimate.impactEffectId;
        }
    }

    private String effectId(int legacyIndex) {
        return registries.effectFromLegacyIndex(legacyIndex).id();
    }

    /** 読込処理を共通ローダーへ委譲し、呼び出し元が同じ失敗形式を受け取れるようにする。 */
    private <T> T read(String path, Class<T> type) {
        return loader.read(path, type);
    }

    public List<CharacterDefinition> characters() {
        return List.copyOf(characters.values());
    }

    public List<AbilityConfigs.ProjectileSkill> skills() {
        return List.copyOf(registries.skillDefinitions.values());
    }

    public List<AbilityConfigs.AreaUltimate> ultimates() {
        return List.copyOf(registries.ultimateDefinitions.values());
    }

    public List<AbilityConfigs.TimedPassive> passives() {
        return List.copyOf(registries.passiveDefinitions.values());
    }

    public AbilityConfigs.ProjectileSkill skill(String id) {
        return registries.skillDefinitions.require(id);
    }

    public AbilityConfigs.AreaUltimate ultimate(String id) {
        return registries.ultimateDefinitions.require(id);
    }

    public AbilityConfigs.TimedPassive passive(String id) {
        return registries.passiveDefinitions.require(id);
    }

    public List<SupportCharacterDefinition> supports() {
        return List.copyOf(supports.values());
    }

    public List<FieldEffectDefinition> fieldEffects() {
        return List.copyOf(fieldEffects.values());
    }

    public SupportCharacterDefinition support(String id) {
        return requireValue(supports, id, "support");
    }

    public FieldEffectDefinition fieldEffect(String id) {
        return requireValue(fieldEffects, id, "field effect");
    }

    private static <T> T requireValue(Map<String, T> values, String id, String type) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + type + ": " + id);
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
