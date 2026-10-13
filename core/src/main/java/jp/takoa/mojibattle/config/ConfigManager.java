package jp.takoa.mojibattle.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;

import java.util.*;

/**
 * 内部JSONからゲーム設定とコンテンツ定義を読み込み、参照整合性と値域を起動時に検証する。
 *
 * <p>読込み順を維持した不変コピーを公開し、ID参照に失敗した場合は設定不備として例外を送出する。</p>
 */
public final class ConfigManager {
    private final Json json = new Json();
    public final GameConfig game;
    public final BattleConfig battle;
    public final GraphicsConfig graphics;
    public final ControlConfig controls;
    public final CriticalConfig critical;
    public final AwakeningConfig awakening;
    public final CustomEditorConfig customEditor;
    private final Map<String, CharacterDefinition> characters = new LinkedHashMap<>();
    private final Map<String, AbilityConfigs.ProjectileSkill> skills = new LinkedHashMap<>();
    private final Map<String, AbilityConfigs.AreaUltimate> ultimates = new LinkedHashMap<>();
    private final Map<String, AbilityConfigs.TimedPassive> passives = new LinkedHashMap<>();
    private final Map<String, SupportCharacterDefinition> supports = new LinkedHashMap<>();
    private final Map<String, FieldEffectDefinition> fieldEffects = new LinkedHashMap<>();

    public ConfigManager() {
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
            require(supports.putIfAbsent(id, support) == null, "Duplicate support: " + id);
        }
        require(game.fieldEffectIds != null, "game.fieldEffectIds is required");

        for (String id : game.fieldEffectIds) {
            FieldEffectDefinition effect = read("config/field_effects/" + id + ".json", FieldEffectDefinition.class);
            require(id.equals(effect.id), "Field-effect filename ID does not match: " + id);
            require(fieldEffects.putIfAbsent(id, effect) == null, "Duplicate field effect: " + id);
        }

        validate();
    }

    private void loadCharacter(String characterId) {
        CharacterDefinition character = read("config/characters/" + characterId + ".json", CharacterDefinition.class);
        character.maxHp *= battle.healthMultiplier;
        AbilityConfigs.ProjectileSkill skill = read("config/abilities/" + characterId + "_skill.json", AbilityConfigs.ProjectileSkill.class);
        AbilityConfigs.AreaUltimate ultimate = read("config/abilities/" + characterId + "_ultimate.json", AbilityConfigs.AreaUltimate.class);
        AbilityConfigs.TimedPassive passive = read("config/abilities/" + characterId + "_passive.json", AbilityConfigs.TimedPassive.class);
        require(characterId.equals(character.id), "Character filename ID does not match: " + characterId);
        require(characters.putIfAbsent(character.id, character) == null, "Duplicate character ID: " + character.id);
        require(skills.putIfAbsent(skill.id, skill) == null, "Duplicate skill ID: " + skill.id);
        require(ultimates.putIfAbsent(ultimate.id, ultimate) == null, "Duplicate ultimate ID: " + ultimate.id);
        require(passives.putIfAbsent(passive.id, passive) == null, "Duplicate passive ID: " + passive.id);
    }

    private <T> T read(String path, Class<T> type) {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            throw new SerializationException("Required config is missing: " + path);
        }

        try {
            return json.fromJson(type, file);
        } catch (RuntimeException error) {
            throw new SerializationException("Invalid config " + path, error);
        }
    }

    private void validate() {
        require(customEditor.maximumSlots > 0 && customEditor.maximumMainPassives > 0 && customEditor.maximumSupportFieldPassives > 0 && customEditor.saveVersion > 0, "custom editor config is invalid");
        require(battle.durationSeconds > 0 && battle.fixedStepSeconds > 0, "battle times must be positive");
        require(battle.skillCooldownMultiplier > 0 && battle.skillCooldownMultiplier <= 1 && battle.healthMultiplier >= 1, "battle pacing multipliers are invalid");
        require(battle.finalMinuteSeconds > 0 && battle.finalMinuteSeconds < battle.durationSeconds, "final minute threshold is invalid");
        require(critical.baseRate >= 0 && critical.baseRate <= 1 && critical.baseDamageMultiplier >= 1, "critical config is invalid");

        require(awakening.spawnAtRemainingSeconds > 0 &&
                awakening.spawnAtRemainingSeconds < battle.durationSeconds &&
                awakening.attackMultiplier >= 1 &&
                awakening.defenseMultiplier >= 1 &&
                awakening.moveSpeedMultiplier >= 1 &&
                awakening.ultimateGainMultiplier >= 1 &&
                awakening.pickupRadius > 0 &&
                awakening.preferredDistance > awakening.pickupRadius &&
                awakening.texturePath != null &&
                Gdx.files.internal(awakening.texturePath).exists(), "awakening config is invalid"
        );

        for (CharacterDefinition character : characters.values()) {
            AbilityConfigs.ProjectileSkill skill = skills.get(character.skillId);
            AbilityConfigs.AreaUltimate ultimate = ultimates.get(character.ultimateId);
            AbilityConfigs.TimedPassive passive = passives.get(character.passiveId);

            require(character.maxHp > 0 && character.moveSpeed > 0 && character.collisionRadius > 0, "Character stats must be positive: " + character.id);
            require(character.type == CharacterType.MAIN, "Playable character must be MAIN: " + character.id);
            require(character.glyphTexture != null && Gdx.files.internal(character.glyphTexture).exists(), "Character glyph texture is missing: " + character.id);
            require(skill != null && ultimate != null && passive != null, "Missing ability reference: " + character.id);
            require(skill.glyphs != null && skill.textures != null && skill.offsets != null && skill.glyphs.length > 0 && skill.glyphs.length == skill.textures.length && skill.glyphs.length == skill.offsets.length, "Projectile arrays must be non-empty and have equal length: " + character.id);

            if (skill.damages != null) {
                require(skill.damages.length == skill.glyphs.length, "Skill damage array length mismatch: " + character.id);
            }

            if (skill.bindDurations != null) {
                require(skill.bindDurations.length == skill.glyphs.length, "Skill bind array length mismatch: " + character.id);
            }

            if ("bind_follow_up".equals(skill.type)) {
                require(skill.glyphs.length >= 2 && skill.followUpTexture != null && skill.followUpDamage > 0 && skill.followUpRadius > 0 && skill.followUpSpeed > 0 && Gdx.files.internal(skill.followUpTexture).exists(), "Follow-up skill config is invalid: " + character.id);
            }
            require(skill.cooldownSeconds >= 0 && skill.projectileLifetimeSeconds > 0 && skill.projectileSpeed > 0, "Skill timing is invalid: " + character.id);

            if ("self_attack_buff".equals(skill.type)) {
                require(skill.buffAttackMultiplier >= 1 && skill.buffDurationSeconds > 0, "Self buff config is invalid: " + character.id);
            }

            if (skill.activationTexture != null) {
                require(skill.activationEffectScale > 0 && Gdx.files.internal(skill.activationTexture).exists(), "Skill activation effect is invalid: " + character.id);
            }

            if (skill.fallingObjectTexture != null) {
                require(skill.fallingDamage > 0 && skill.fallingRadius > 0 && skill.fallingWarningSeconds >= 0 && skill.fallingEffectSeconds > 0 && skill.fallingSpawnHeight > 0 && Gdx.files.internal(skill.fallingObjectTexture).exists(), "Falling skill object is invalid: " + character.id);
            }

            require(skill.healthCostRatio >= 0 && skill.healthCostRatio < 1, "Skill health cost is invalid: " + character.id);

            for (String texture : skill.textures) {
                require(texture != null && Gdx.files.internal(texture).exists(), "Skill texture is missing: " + character.id);
            }

            require(ultimate.pointCost > 0 && ultimate.warningSeconds >= 0 && ultimate.nameKey != null && Set.of("area", "self_heal", "line", "meteor_shower", "summon_family", "moving_car", "spinning_one", "skill_barrage", "sushi_barrage").contains(ultimate.type), "Ultimate config is invalid: " + character.id);
            require(ultimate.objectTexture != null && Gdx.files.internal(ultimate.objectTexture).exists(), "Ultimate object texture is missing: " + character.id);

            if (ultimate.type.equals("area")) {
                require(ultimate.radius > 0, "Area ultimate radius is invalid: " + character.id);
            }

            if (ultimate.burnDuration > 0) {
                require(ultimate.burnDamage > 0 && ultimate.burnTickInterval > 0 && ultimate.burnUltimatePointsPerTick >= 0, "Burn config is invalid: " + character.id);
            }

            if (ultimate.type.equals("self_heal")) {
                require(ultimate.healAmount > 0, "Heal amount is invalid: " + character.id);
            }

            if (ultimate.type.equals("line")) {
                require(ultimate.lineLength > 0 && ultimate.lineWidth > 0, "Line size is invalid: " + character.id);
            }

            if (ultimate.type.equals("meteor_shower")) {
                require(ultimate.meteorCount > 0 && ultimate.radius > 0, "Meteor config is invalid: " + character.id);
            }

            if (ultimate.type.equals("summon_family")) {
                require(ultimate.summonCount > 0 && ultimate.summonHp > 0 && ultimate.summonDefense >= 0 && ultimate.summonAttackDamage > 0 && ultimate.summonAttackInterval > 0 && ultimate.summonProjectileSpeed > 0, "Family summon config is invalid: " + character.id);
            }

            if (ultimate.type.equals("moving_car")) {
                require(ultimate.radius > 0 && ultimate.movementSpeed > 0 && ultimate.stunSeconds > 0 && ultimate.effectSeconds > 0, "Moving car config is invalid: " + character.id);
            }

            if (ultimate.type.equals("spinning_one")) {
                require(ultimate.lineLength > 0 && ultimate.lineWidth > 0 && ultimate.healAmount > 0 && ultimate.effectSeconds > 0, "Spinning one config is invalid: " + character.id);
            }

            if (ultimate.type.equals("skill_barrage")) {
                require(ultimate.barrageCount > 0 && ultimate.barrageInterval > 0 && skills.containsKey(ultimate.barrageSkillId), "Skill barrage invalid: " + character.id);
            }

            if (ultimate.type.equals("sushi_barrage")) {
                require(ultimate.barrageCount > 0 && ultimate.barrageInterval > 0 && ultimate.projectileSpeed > 0 && ultimate.projectileLifetimeSeconds > 0 && ultimate.projectileRadius > 0, "Sushi barrage config is invalid: " + character.id);
            }

            require(passive.intervalSeconds > 0 && passive.maximumMultiplier >= 1 && passive.type != null && Set.of("compound_interest", "hitbox_boost", "ultimate_regen", "damage_guard", "speed_boost", "ultimate_gain", "stun_boost", "skill_haste", "family_bond", "difficult_kanji", "berserker", "ultimate_boost").contains(passive.type), "Passive config is invalid: " + character.id);
            require(passive.moveSpeedMultiplier > 0 && passive.skillCooldownMultiplier > 0 && passive.ultimateGainMultiplier > 0 && passive.incomingDamageMultiplier > 0 && passive.hitboxMultiplier > 0 && passive.stunMultiplier > 0 && passive.ultimatePointsPerSecond >= 0, "Passive multipliers are invalid: " + character.id);

            if ("difficult_kanji".equals(passive.type)) {
                require(passive.procChance >= 0 && passive.procChance <= 1 && passive.procStunSeconds > 0 && passive.procInternalCooldown >= 0, "Difficult-kanji proc config is invalid: " + character.id);
            }

            if ("berserker".equals(passive.type)) {
                require(passive.missingHealthAttackBonus > 0, "Berserker passive is invalid: " + character.id);
            }

            if ("ultimate_boost".equals(passive.type)) {
                require(passive.ultimateDamageMultiplier >= 1, "Ultimate boost passive is invalid: " + character.id);
            }
        }

        for (SupportCharacterDefinition support : supports.values()) {
            require(support.type == CharacterType.SUPPORT && support.moveSpeed > 0 && support.followDistance > 0 && support.attackRange > 0 && support.attackInterval > 0 && support.projectileSpeed > 0, "Support stats invalid: " + support.id);
            require(support.skill != null && support.ultimate != null && support.fieldPassiveIds != null && support.fieldPassiveIds.length > 0 && support.ultimateChargePerSecond > 0, "Support abilities invalid: " + support.id);

            require(Gdx.files.internal(support.glyphTexture).exists() && Gdx.files.internal(support.projectileTexture).exists(), "Support texture missing: " + support.id);

            require(Set.of("ally_attack_up", "enemy_attack_down", "enemy_slow", "bonus_damage", "wind_pull").contains(support.skill.type), "Unknown support skill type: " + support.id);
            require(Set.of("enemy_defense_down", "heal_cleanse", "wide_slash", "right_teleport", "global_cooldown_halve").contains(support.ultimate.type), "Unknown support ultimate type: " + support.id);

            require(support.skill.cooldownSeconds > 0 && support.skill.durationSeconds >= 0 && support.ultimate.pointCost == 100, "Support ability values invalid: " + support.id);

            for (String effect : support.fieldPassiveIds) {
                require(fieldEffects.containsKey(effect), "Unknown field effect: " + effect);
            }
        }
        for (FieldEffectDefinition effect : fieldEffects.values()) {
            require(effect.id != null && effect.type != null && effect.target != null, "Field effect fields are required");
            require(Set.of("critical_enable", "cooldown", "move_multiplier", "periodic_heal", "damage_taken", "speed_pulse", "attack_growth", "projectile_curve", "traffic").contains(effect.type), "Unknown field-effect type: " + effect.id);
            require(effect.magnitude >= 0 && effect.intervalSeconds >= 0 && effect.durationSeconds >= 0 && effect.maximum >= 0, "Field-effect values invalid: " + effect.id);
        }

        Set<String> keys = new HashSet<>();
        addBindings(keys, controls.playerOne);
        addBindings(keys, controls.playerTwo);
        addBindings(keys, controls.playerThree);
        addBindings(keys, controls.playerFour);

        require(keys.add(controls.pause), "duplicate key binding: " + controls.pause);
    }

    public List<CharacterDefinition> characters() {
        return List.copyOf(characters.values());
    }

    public List<AbilityConfigs.ProjectileSkill> skills() {
        return List.copyOf(skills.values());
    }

    public List<AbilityConfigs.AreaUltimate> ultimates() {
        return List.copyOf(ultimates.values());
    }

    public List<AbilityConfigs.TimedPassive> passives() {
        return List.copyOf(passives.values());
    }

    public AbilityConfigs.ProjectileSkill skill(String id) {
        return requireValue(skills, id, "skill");
    }

    public AbilityConfigs.AreaUltimate ultimate(String id) {
        return requireValue(ultimates, id, "ultimate");
    }

    public AbilityConfigs.TimedPassive passive(String id) {
        return requireValue(passives, id, "passive");
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

    private static void addBindings(Set<String> keys, ControlConfig.Binding b) {
        require(b != null, "control binding is missing");

        for (String key : new String[]{b.forward, b.backward, b.left, b.right, b.skill, b.ultimate, b.supportSkill, b.supportUltimate}) {
            require(key != null && keys.add(key), "missing or duplicate key binding: " + key);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
