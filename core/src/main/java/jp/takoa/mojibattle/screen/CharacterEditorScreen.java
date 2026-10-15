package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.custom.CustomCharacterData;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterType;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.registry.AbilityPresentationFormatter;

import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * 既存の外観・スキル・必殺技・パッシブを段階的に組み合わせ、カスタムキャラクターを保存する画面。
 *
 * <p>各段階では設定またはレジストリに存在するIDだけを選択させ、最後に指定された保存枠へ
 * {@link CustomCharacterData} を永続化する。長い説明を持つ候補はスクロール領域内へ表示する。</p>
 */
public final class CharacterEditorScreen extends StageScreen {
    private static final int CHOICE_COLUMNS = 2;
    private static final float CHOICE_WIDTH = 430f;
    private static final float CHOICE_HEIGHT = 124f;
    private static final float PASSIVE_CHOICE_HEIGHT = 136f;

    private final Table root = new Table();
    private final Table content = new Table();
    private final Label heading;
    private final Label help;
    private final Label error;
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();

    private CharacterType type;
    private String appearanceId;
    private String skillId;
    private String ultimateId;
    private String displayName = "";
    private int step;

    /**
     * 共有設定と保存領域を使用して編集ウィザードを構築する。
     */
    public CharacterEditorScreen(GameContext context, ScreenManager screens) {
        super(context, screens);
        root.setFillParent(true);
        root.pad(18);

        Table panel = new Table(context.skin);
        panel.setBackground("panel");
        panel.pad(20);

        heading = new Label(context.i18n.text("editor.title"), context.skin, "title");
        heading.setFontScale(.48f);

        help = new Label("", context.skin);
        help.setWrap(true);
        help.setAlignment(Align.center);

        error = new Label(initialErrorMessage(), context.skin);
        error.setColor(1, .45f, .35f, 1);
        error.setWrap(true);

        ScrollPane choices = new ScrollPane(content);
        choices.setFadeScrollBars(false);
        choices.setScrollingDisabled(false, false);
        choices.setOverscroll(false, false);

        panel.add(heading).growX().height(70).row();
        panel.add(help).growX().height(46).padBottom(6).row();
        panel.add(error).growX().height(38).row();
        panel.add(choices).grow().minWidth(0).minHeight(0);

        addStandardMenu(root, panel);
        rebuild();
    }

    private String initialErrorMessage() {
        String loadError = context.customCharacters.loadError();
        return loadError == null ? "" : context.i18n.text("editor.invalid_save", loadError);
    }

    /**
     * 現在の編集段階に対応する候補だけを再構築する。
     */
    private void rebuild() {
        content.clearChildren();
        error.setText("");
        switch (step) {
            case 0 -> typeStep();
            case 1 -> appearanceStep();
            case 2 -> skillStep();
            case 3 -> ultimateStep();
            case 4 -> passiveStep();
            default -> slotStep();
        }
    }

    private void typeStep() {
        help.setText(context.i18n.text("editor.choose_type"));

        option(context.i18n.text("editor.main"), () -> {
            type = CharacterType.MAIN;
            step = 1;
            rebuild();
        });

        option(context.i18n.text("editor.support"), () -> {
            type = CharacterType.SUPPORT;
            step = 1;
            rebuild();
        });

        backToMenu();
    }

    private void appearanceStep() {
        help.setText(context.i18n.text("editor.choose_appearance"));

        TextField name = new TextField(displayName, context.skin);
        name.setMessageText(context.i18n.text("editor.name_hint"));
        name.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                displayName = name.getText();
            }
        });

        content.add(new Label(context.i18n.text("editor.display_name"), context.skin)).width(180);
        content.add(name).width(650).height(48).row();

        int index = 0;

        if (type == CharacterType.MAIN) {
            for (CharacterDefinition value : context.config.characters()) {
                addChoice(value.id, value.displayGlyph + "  " + context.i18n.text(value.nameKey), appearanceId, () -> appearanceId = value.id);
                index = endChoiceRow(index);
            }
        } else {
            for (SupportCharacterDefinition value : context.config.supports()) {
                addChoice(value.id, value.displayGlyph + "  " + context.i18n.text(value.nameKey), appearanceId, () -> appearanceId = value.id);
                index = endChoiceRow(index);
            }
        }
        navigation(0, () -> {
            if (appearanceId != null) {
                step = 2;
                rebuild();
            }
        });
    }

    private void skillStep() {
        help.setText(context.i18n.text("editor.choose_skill"));

        if (type == CharacterType.MAIN) {
            addMainSkillChoices();
        } else {
            addSupportSkillChoices();
        }

        navigation(1, () -> {
            if (skillId != null) {
                step = 3;
                rebuild();
            }
        });
    }

    private void addMainSkillChoices() {
        int index = 0;

        for (CharacterDefinition owner : context.config.characters()) {
            AbilityConfigs.ProjectileSkill ability = context.config.skill(owner.skillId);

            String summary = AbilityPresentationFormatter.format(context.registries.skillImplementations.require(ability.implementationId).presentation().apply(ability, context.config.battle.skillCooldownMultiplier), context.i18n);
            String label = context.i18n.text("editor.ability_option", context.i18n.text(ability.nameKey), context.i18n.text(owner.nameKey), summary);

            addChoice(ability.id, label, skillId, () -> skillId = ability.id);
            index = endChoiceRow(index);
        }
    }

    private void addSupportSkillChoices() {
        int index = 0;

        for (SupportCharacterDefinition owner : context.config.supports()) {
            String summary = context.i18n.text("editor.support_skill_summary", String.format("%.1f", cooldown(owner.skill.cooldownSeconds)), context.i18n.text(owner.skill.descriptionKey));
            String label = context.i18n.text("editor.ability_option", context.i18n.text(owner.skill.nameKey), context.i18n.text(owner.nameKey), summary);

            addChoice(owner.skill.id, label, skillId, () -> skillId = owner.skill.id);
            index = endChoiceRow(index);
        }
    }

    private void ultimateStep() {
        help.setText(context.i18n.text("editor.choose_ultimate"));

        if (type == CharacterType.MAIN) {
            addMainUltimateChoices();
        } else {
            addSupportUltimateChoices();
        }

        navigation(2, () -> {
            if (ultimateId != null) {
                selected.clear();
                step = 4;
                rebuild();
            }
        });
    }

    private void addMainUltimateChoices() {
        int index = 0;

        for (CharacterDefinition owner : context.config.characters()) {
            AbilityConfigs.AreaUltimate ability = context.config.ultimate(owner.ultimateId);

            String summary = context.i18n.text("editor.ultimate_summary", ability.type, Math.round(ability.baseDamage), Math.round(ability.pointCost));
            String label = context.i18n.text("editor.ability_option", context.i18n.text(ability.nameKey), context.i18n.text(owner.nameKey), summary);

            addChoice(ability.id, label, ultimateId, () -> ultimateId = ability.id);
            index = endChoiceRow(index);
        }
    }

    private void addSupportUltimateChoices() {
        int index = 0;

        for (SupportCharacterDefinition owner : context.config.supports()) {
            String label = context.i18n.text("editor.ability_option", context.i18n.text(owner.ultimate.nameKey), context.i18n.text(owner.nameKey), context.i18n.text("select.support_ultimate_detail." + owner.id));

            addChoice(owner.ultimate.id, label, ultimateId, () -> ultimateId = owner.ultimate.id);
            index = endChoiceRow(index);
        }
    }

    private void passiveStep() {
        int limit = type == CharacterType.MAIN ? context.config.customEditor.maximumMainPassives : context.config.customEditor.maximumSupportFieldPassives;

        String helpKey = type == CharacterType.MAIN ? "editor.choose_passive" : "editor.choose_field_passive";
        help.setText(context.i18n.text(helpKey, selected.size(), limit));

        if (type == CharacterType.MAIN) {
            addMainPassiveChoices(limit);
        } else {
            addFieldPassiveChoices(limit);
        }

        navigation(3, () -> {
            if (!selected.isEmpty()) {
                step = 5;
                rebuild();
            }
        });
    }

    private void addMainPassiveChoices(int limit) {
        int index = 0;

        for (CharacterDefinition owner : context.config.characters()) {
            String id = owner.passiveId;
            String label = context.i18n.text("editor.passive_option", context.i18n.text("ability." + owner.id + ".passive.name"), context.i18n.text(owner.nameKey));

            label += "\n" + context.i18n.text("guide.passive_effect." + owner.id);

            toggleChoice(id, label, limit);
            index = endChoiceRow(index);
        }
    }

    private void addFieldPassiveChoices(int limit) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();

        for (SupportCharacterDefinition owner : context.config.supports()) {
            ids.addAll(Arrays.asList(owner.fieldPassiveIds));
        }

        int index = 0;

        for (String id : ids) {
            String label = context.i18n.text("field." + id) + "\n" + context.i18n.text("field.description." + id);
            toggleChoice(id, label, limit);
            index = endChoiceRow(index);
        }
    }

    private int endChoiceRow(int previousIndex) {
        int nextIndex = previousIndex + 1;

        if (nextIndex % CHOICE_COLUMNS == 0) {
            content.row();
        }

        return nextIndex;
    }

    private void slotStep() {
        help.setText(context.i18n.text("editor.choose_slot"));

        for (int slot = 1; slot <= context.config.customEditor.maximumSlots; slot++) {
            CustomCharacterData saved = context.customCharacters.slot(slot);
            String label = saved == null ? context.i18n.text("editor.slot_empty", slot) : savedSlotLabel(slot, saved);

            int chosen = slot;
            option(label, () -> chooseSlot(chosen));

            if (slot % 2 == 0) {
                content.row();
            }
        }

        navigation(4, () -> {});
    }

    private String savedSlotLabel(int slot, CustomCharacterData saved) {
        String savedName = saved.displayName == null || saved.displayName.isBlank() ? "Custom " + slot : saved.displayName;
        String typeName = saved.type == CharacterType.MAIN ? context.i18n.text("editor.main") : context.i18n.text("editor.support");

        return context.i18n.text("editor.slot_used", slot, savedName, typeName, glyph(saved));
    }

    private void chooseSlot(int slot) {
        if (context.customCharacters.slot(slot) != null) {
            content.clearChildren();
            help.setText(context.i18n.text("editor.overwrite_confirm"));

            option(context.i18n.text("editor.overwrite"), () -> save(slot));
            option(context.i18n.text("select.cancel"), this::rebuild);
            return;
        }

        save(slot);
    }

    /**
     * 選択したID群を保存し、対応する実行時レジストリの同じ枠を置き換える。
     */
    private void save(int slot) {
        try {
            CustomCharacterData data = new CustomCharacterData();
            data.slot = slot;
            data.version = context.config.customEditor.saveVersion;
            data.id = "custom_" + slot;
            data.type = type;
            data.displayName = displayName;
            data.appearanceId = appearanceId;

            if (type == CharacterType.MAIN) {
                data.skillId = skillId;
                data.ultimateId = ultimateId;
                data.passiveIds = selected.toArray(String[]::new);

            } else {
                data.supportSkillId = skillId;
                data.supportUltimateId = ultimateId;
                data.fieldPassiveIds = selected.toArray(String[]::new);
            }

            context.customCharacters.save(data);
            context.characters.removeCustomSlot(slot);
            context.supports.removeCustomSlot(slot);

            if (type == CharacterType.MAIN) {
                context.characters.replaceCustom(context.customCharacters.createMain(data));
            } else {
                context.supports.replaceCustom(context.customCharacters.createSupport(data));
            }

            screens.show(new TitleScreen(context, screens, "editor.save_success"));

        } catch (RuntimeException failure) {
            error.setText(context.i18n.text("editor.save_failed", failure.getMessage()));
        }
    }

    private String glyph(CustomCharacterData data) {
        if (data.type == CharacterType.MAIN) {
            return context.config.characters().stream().filter(value -> value.id.equals(data.appearanceId)).map(value -> value.displayGlyph).findFirst().orElse("?");
        }

        return context.config.supports().stream().filter(value -> value.id.equals(data.appearanceId)).map(value -> value.displayGlyph).findFirst().orElse("?");
    }

    private void addChoice(String id, String text, String current, Runnable choose) {
        String marker = id.equals(current) ? "▶ " : "";
        TextButton button = new TextButton(marker + text, context.skin);

        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(Align.center);

        content.add(button).width(CHOICE_WIDTH).height(CHOICE_HEIGHT).pad(7);

        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                choose.run();
                rebuild();
            }
        });
    }

    private void toggleChoice(String id, String text, int limit) {
        String marker = selected.contains(id) ? "✓ " : "";
        TextButton button = new TextButton(marker + text, context.skin);

        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(Align.center);

        content.add(button).width(CHOICE_WIDTH).height(PASSIVE_CHOICE_HEIGHT).pad(7);

        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if (selected.contains(id)) {
                    selected.remove(id);
                } else if (selected.size() < limit) {
                    selected.add(id);
                }
                rebuild();
            }
        });
    }

    private void option(String text, Runnable action) {
        TextButton button = new TextButton(text, context.skin);
        button.getLabel().setWrap(true);

        content.add(button).width(430).height(68).pad(7);
        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }

    private void navigation(int previous, Runnable next) {
        content.row();

        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        TextButton forward = new TextButton(context.i18n.text("editor.next"), context.skin);

        content.add(back).width(300).height(58).padTop(12);
        content.add(forward).width(300).height(58).padTop(12);

        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                step = previous;
                rebuild();
            }
        });
        forward.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                next.run();
            }
        });
    }

    private void backToMenu() {
        content.row();

        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        content.add(back).colspan(2).width(300).height(58).padTop(20);

        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });
    }

    private float cooldown(float value) {
        return value * context.config.battle.skillCooldownMultiplier;
    }
}
