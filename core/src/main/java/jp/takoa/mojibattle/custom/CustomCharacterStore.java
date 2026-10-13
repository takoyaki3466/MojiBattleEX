package jp.takoa.mojibattle.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.support.SupportAbilityDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * プレイヤー作成キャラクターをバージョン付きJSONとして一時ファイル経由で安全に保存する。
 */
public final class CustomCharacterStore {
    private static final String SAVE_PATH = "MojiBattleEX/custom-characters.json";

    private final ConfigManager config;
    private final Json json = new Json();
    private final Map<Integer, CustomCharacterData> slots = new TreeMap<>();

    private String loadError;

    public CustomCharacterStore(ConfigManager config) {
        this.config = config;
        load();
    }

    /**
     * @return 保存枠番号順に並んだ全データの変更不能なコピー
     */
    public Collection<CustomCharacterData> all() {
        return List.copyOf(slots.values());
    }

    public CustomCharacterData slot(int number) {
        return slots.get(number);
    }

    public String loadError() {
        return loadError;
    }

    /**
     * 1枠分のデータを検証し、一時ファイルを検証してから本体ファイルへ置き換える。
     *
     * @throws IllegalStateException ファイルの書込みまたは置換に失敗した場合
     */
    public void save(CustomCharacterData data) {
        validate(data);

        Map<Integer, CustomCharacterData> next = new TreeMap<>(slots);
        next.put(data.slot, data);

        CustomCharacterSave document = new CustomCharacterSave();
        document.version = config.customEditor.saveVersion;
        document.slots = next.values().toArray(CustomCharacterData[]::new);

        FileHandle target = Gdx.files.external(SAVE_PATH);
        FileHandle temporary = Gdx.files.external(SAVE_PATH + ".tmp");

        try {
            target.parent().mkdirs();

            temporary.writeString(json.prettyPrint(document), false, "UTF-8");
            CustomCharacterSave verification = json.fromJson(CustomCharacterSave.class, temporary);

            if (verification == null || verification.slots == null) {
                throw new IllegalStateException("Saved JSON could not be verified");
            }
            try {
                Files.move(temporary.file().toPath(), target.file().toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.file().toPath(), target.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            slots.clear();
            slots.putAll(next);

        } catch (Exception error) {
            temporary.delete();
            throw new IllegalStateException("Custom character save failed", error);
        }
    }

    private void load() {
        FileHandle file = Gdx.files.external(SAVE_PATH);

        if (!file.exists()) {
            return;
        }

        try {
            CustomCharacterSave document = json.fromJson(CustomCharacterSave.class, file);

            if (document == null || document.version != config.customEditor.saveVersion || document.slots == null) {
                throw new IllegalArgumentException("Unsupported or missing save version");
            }

            for (CustomCharacterData data : document.slots) {
                validate(data);

                if (slots.putIfAbsent(data.slot, data) != null) {
                    throw new IllegalArgumentException("Duplicate slot " + data.slot);
                }
            }

        } catch (RuntimeException error) {
            slots.clear();
            loadError = error.getMessage();
        }
    }

    private void validate(CustomCharacterData data) {
        if (data == null || data.slot < 1 || data.slot > config.customEditor.maximumSlots) {
            fail("Invalid slot");
        }

        if (data.version != config.customEditor.saveVersion) {
            fail("Invalid character data version");
        }

        if (data.type == null || data.appearanceId == null || data.id == null || data.id.isBlank()) {
            fail("Required custom character field is missing");
        }

        if (!data.id.equals("custom_" + data.slot)) {
            fail("Custom character ID does not match its slot");
        }

        if (data.type == CharacterType.MAIN) {
            requireMain(data.appearanceId);

            config.skill(data.skillId);
            config.ultimate(data.ultimateId);

            requireDistinct(data.passiveIds, config.customEditor.maximumMainPassives, "passives");

            for (String id : data.passiveIds) {
                config.passive(id);
            }

        } else {
            requireSupport(data.appearanceId);
            findSupportSkill(data.supportSkillId);
            findSupportUltimate(data.supportUltimateId);
            requireDistinct(data.fieldPassiveIds, config.customEditor.maximumSupportFieldPassives, "field passives");

            for (String id : data.fieldPassiveIds) {
                config.fieldEffect(id);
            }
        }
    }

    /**
     * 保存データと選択された外観を合成し、メインキャラクター定義を生成する。
     */
    public CharacterDefinition createMain(CustomCharacterData data) {
        CharacterDefinition appearance = requireMain(data.appearanceId);
        CharacterDefinition value = new CharacterDefinition();

        value.id = data.id;
        value.type = CharacterType.MAIN;
        value.displayGlyph = appearance.displayGlyph;
        value.uiLabel = appearance.uiLabel;
        value.displayName = defaultName(data);
        value.appearanceId = appearance.id;
        value.custom = true;
        value.nameKey = appearance.nameKey;
        value.customSlot = data.slot;
        value.maxHp = appearance.maxHp;
        value.baseAttack = appearance.baseAttack;
        value.moveSpeed = appearance.moveSpeed;
        value.collisionRadius = appearance.collisionRadius;
        value.skillId = data.skillId;
        value.ultimateId = data.ultimateId;
        value.passiveIds = data.passiveIds.clone();
        value.passiveId = data.passiveIds[0];
        value.glyphTexture = appearance.glyphTexture;
        value.criticalRate = appearance.criticalRate;
        value.criticalDamageMultiplier = appearance.criticalDamageMultiplier;
        value.criticalEnabled = appearance.criticalEnabled;
        value.zeroDefense = appearance.zeroDefense;

        return value;
    }

    /**
     * 保存データと選択された外観を合成し、補助キャラクター定義を生成する。
     */
    public SupportCharacterDefinition createSupport(CustomCharacterData data) {
        SupportCharacterDefinition appearance = requireSupport(data.appearanceId);
        SupportCharacterDefinition value = new SupportCharacterDefinition();

        value.id = data.id;
        value.type = CharacterType.SUPPORT;
        value.displayGlyph = appearance.displayGlyph;
        value.displayName = defaultName(data);
        value.appearanceId = appearance.id;
        value.custom = true;
        value.nameKey = appearance.nameKey;
        value.customSlot = data.slot;
        value.glyphTexture = appearance.glyphTexture;
        value.projectileTexture = appearance.projectileTexture;
        value.moveSpeed = appearance.moveSpeed;
        value.followDistance = appearance.followDistance;
        value.attackRange = appearance.attackRange;
        value.attackInterval = appearance.attackInterval;
        value.baseDamage = appearance.baseDamage;
        value.projectileSpeed = appearance.projectileSpeed;
        value.ultimateChargePerSecond = appearance.ultimateChargePerSecond;
        value.criticalRate = appearance.criticalRate;
        value.criticalDamageMultiplier = appearance.criticalDamageMultiplier;
        value.criticalEnabled = appearance.criticalEnabled;
        value.curvedProjectile = appearance.curvedProjectile;
        value.skill = findSupportSkill(data.supportSkillId);
        value.ultimate = findSupportUltimate(data.supportUltimateId);
        value.fieldPassiveIds = data.fieldPassiveIds.clone();

        return value;
    }

    private CharacterDefinition requireMain(String id) {
        return config.characters().stream().filter(value -> value.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown main appearance: " + id));
    }

    private SupportCharacterDefinition requireSupport(String id) {
        return config.supports().stream().filter(value -> value.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown support appearance: " + id));
    }

    private SupportAbilityDefinition findSupportSkill(String id) {
        return config.supports().stream().map(value -> value.skill).filter(value -> value.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown support skill: " + id));
    }

    private SupportAbilityDefinition findSupportUltimate(String id) {
        return config.supports().stream().map(value -> value.ultimate).filter(value -> value.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown support ultimate: " + id));
    }

    private String defaultName(CustomCharacterData data) {
        return data.displayName == null || data.displayName.isBlank() ? "Custom " + data.slot : data.displayName.trim();
    }

    private static void requireDistinct(String[] ids, int limit, String label) {
        boolean invalid = ids == null || ids.length == 0 || ids.length > limit || new HashSet<>(Arrays.asList(ids)).size() != ids.length;

        if (invalid) {
            fail("Invalid " + label);
        }

        for (String id : ids) {
            if (id == null || id.isBlank()) {
                fail("Invalid " + label);
            }
        }
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }
}
