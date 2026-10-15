package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.registry.AbilityPresentationFormatter;
import jp.takoa.mojibattle.resource.GdxTextureLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * プレイヤーごとにメインと補助キャラクターを選び、詳細確認後に戦闘を開始する画面。
 */
public final class CharacterSelectScreen extends StageScreen {
    private static final float MENU_WIDTH_RATIO = .985f;
    private static final float MENU_HEIGHT_RATIO = .97f;
    private static final int CARD_COLUMNS = 5;

    private final BattleMode mode;
    private final int playerCount;
    private final GdxTextureLoader textureLoader;
    private final Label prompt;
    private final Label chosen;
    private final Label details;

    private final Image preview = new Image();
    private final Table cardGrid = new Table();
    private final Table playerPreviews = new Table();
    private final Table supportCountControls = new Table();

    private final TextButton existingTab;
    private final TextButton customTab;
    private final TextButton confirm;
    private final TextButton cancel;

    private final Map<String, Texture> previewTextures = new HashMap<>();
    private final List<String> mainIds = new ArrayList<>();
    private final List<String> supportIds = new ArrayList<>();

    private Table selectedCard;
    private String pendingId;
    private int phase;
    private int supportCount = 1;
    private boolean showingCustom;

    public CharacterSelectScreen(GameContext context, ScreenManager screens, BattleMode mode, int playerCount) {
        super(context, screens);
        this.mode = mode;
        textureLoader = new GdxTextureLoader(context.resources);
        this.playerCount = mode == BattleMode.TRAINING ? 1 : Math.max(2, Math.min(4, playerCount));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(0);
        Table roster = new Table(context.skin);
        roster.setBackground("panel");
        roster.pad(10);

        Label heading = new Label(context.i18n.text("select.title"), context.skin, "title");
        heading.setFontScale(.48f);
        roster.add(heading).width(705).height(58).left().row();

        prompt = new Label("", context.skin);
        prompt.setColor(.62f, 1f, .76f, 1f);
        roster.add(prompt).left().row();

        chosen = new Label("", context.skin);
        chosen.setFontScale(.62f);
        roster.add(chosen).left().padBottom(8).row();

        Label supportCountLabel = new Label(context.i18n.text("select.support_count"), context.skin);
        TextButton oneSupport = new TextButton(context.i18n.text("select.support_count_one"), context.skin);
        TextButton twoSupports = new TextButton(context.i18n.text("select.support_count_two"), context.skin);

        supportCountControls.add(supportCountLabel).padRight(8);
        supportCountControls.add(oneSupport).width(150).height(42).padRight(4);
        supportCountControls.add(twoSupports).width(150).height(42).padLeft(4);
        supportCountControls.setVisible(false);

        roster.add(supportCountControls).left().height(46).row();
        oneSupport.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                setSupportCount(1);
            }
        });
        twoSupports.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                setSupportCount(2);
            }
        });

        Table tabs = new Table();
        existingTab = new TextButton(context.i18n.text("select.existing"), context.skin);
        customTab = new TextButton(context.i18n.text("select.custom"), context.skin);

        tabs.add(existingTab).width(260).height(52).padRight(5);
        tabs.add(customTab).width(260).height(52).padLeft(5);

        roster.add(tabs).left().padBottom(5).row();
        roster.add(cardGrid).width(705).height(302).top().left();

        Table information = new Table(context.skin);
        information.setBackground("panel");
        information.pad(12);

        preview.setScaling(Scaling.fit);
        information.add(preview).size(145, 145).row();

        details = new Label(context.i18n.text("select.choose_card_hint"), context.skin);
        details.setWrap(true);
        details.setAlignment(Align.left, Align.top);
        details.setFontScale(.59f);
        information.add(details).width(455).height(292).top().left().padTop(6).row();

        Table decisions = new Table();
        cancel = new TextButton(context.i18n.text("select.cancel"), context.skin);
        confirm = new TextButton(context.i18n.text("select.confirm"), context.skin);

        confirm.setDisabled(true);

        decisions.add(cancel).width(180).height(50).padRight(5);
        decisions.add(confirm).width(200).height(50).padLeft(5);

        information.add(decisions).padTop(6);

        cancel.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                clearCandidate();
            }
        });
        confirm.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                confirmCandidate();
            }
        });

        Table upper = new Table();
        upper.add(roster).width(735).height(548).top().padRight(5);
        upper.add(information).width(495).height(548).top().padLeft(5);

        Table shell = new Table(context.skin);
        shell.setBackground("bar-bg");
        shell.pad(5);
        shell.add(upper).growX().top().row();
        shell.add(playerPreviews).growX().height(126).padTop(3);

        addMenu(root, shell, MENU_WIDTH_RATIO, MENU_HEIGHT_RATIO);

        existingTab.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                showingCustom = false;
                refreshCards();
            }
        });
        customTab.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                showingCustom = true;
                refreshCards();
            }
        });

        updatePrompt();
        refreshCards();
    }

    private void refreshCards() {
        cardGrid.clearChildren();

        selectedCard = null;
        pendingId = null;

        confirm.setDisabled(true);
        preview.setDrawable(null);

        details.setText(context.i18n.text("select.choose_card_hint"));
        existingTab.setText((showingCustom ? "" : "▶ ") + context.i18n.text("select.existing"));
        customTab.setText((showingCustom ? "▶ " : "") + context.i18n.text("select.custom"));

        int index = selectingMain() ? addMainCards() : addSupportCards();

        if (index == 0) {
            Label empty = new Label(context.i18n.text("select.custom_empty"), context.skin);
            empty.setWrap(true);
            cardGrid.add(empty).width(650).height(120).center();
        }
        refreshPlayerPreviews();
    }

    private int addMainCards() {
        int index = 0;

        for (CharacterDefinition character : context.characters.all()) {
            if (character.custom != showingCustom) {
                continue;
            }

            String glyph = character.uiLabel == null ? character.displayGlyph : character.uiLabel;
            addCard(character.id, character.glyphTexture, glyph, name(character), () -> showMain(character));

            index++;

            if (index % CARD_COLUMNS == 0) {
                cardGrid.row();
            }
        }
        return index;
    }

    private int addSupportCards() {
        int index = 0;

        for (SupportCharacterDefinition support : context.supports.all()) {
            if (support.custom != showingCustom) {
                continue;
            }

            addCard(support.id, support.glyphTexture, support.displayGlyph, name(support), () -> showSupport(support));

            index++;

            if (index % CARD_COLUMNS == 0) {
                cardGrid.row();
            }
        }
        return index;
    }

    private void addCard(String id, String texturePath, String glyph, String summary, Runnable detailsAction) {
        Table card = new Table(context.skin);
        card.setBackground("button");
        card.pad(6);
        card.setTouchable(Touchable.enabled);

        Image image = new Image(drawable(texturePath));
        image.setScaling(Scaling.fit);

        Label glyphLabel = new Label(glyph, context.skin);
        glyphLabel.setFontScale(.7f);
        glyphLabel.setAlignment(Align.center);

        Label summaryLabel = new Label(summary, context.skin);
        summaryLabel.setWrap(true);
        summaryLabel.setAlignment(Align.center);
        summaryLabel.setFontScale(.47f);

        card.add(image).size(46, 46).row();
        card.add(glyphLabel).width(118).height(20).row();
        card.add(summaryLabel).width(118).height(28).top();

        card.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (selectedCard != null) {
                    selectedCard.setBackground("button");
                }

                selectedCard = card;
                card.setBackground("button-down");
                pendingId = id;
                confirm.setDisabled(false);
                detailsAction.run();

                refreshPlayerPreviews();
                return true;
            }
        });

        cardGrid.add(card).width(132).height(108).pad(4).top();
    }

    private TextureRegionDrawable drawable(String path) {
        Texture texture = previewTextures.computeIfAbsent(path, key -> {
            Texture created = textureLoader.load(key, true);
            created.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            return created;
        });

        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void showTexture(String path) {
        preview.setDrawable(drawable(path));
    }

    private void showMain(CharacterDefinition character) {
        showTexture(character.glyphTexture);
        AbilityConfigs.ProjectileSkill skill = context.config.skill(character.skillId);
        AbilityConfigs.AreaUltimate ultimate = context.config.ultimate(character.ultimateId);

        String style = character.custom ? context.i18n.text("select.custom_style") : context.i18n.text("guide.character." + character.id);

        details.setText("- " + context.i18n.text("guide.skill", context.i18n.text(skill.nameKey)) + "\n  " + skillDetail(skill) + "\n- " + context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey)) + "\n  " + ultimateDetail(ultimate) + " / " + context.i18n.text("guide.cost", Math.round(ultimate.pointCost)) + "\n- " + context.i18n.text("guide.passive", passiveNames(character)) + "\n  " + passiveDescriptions(character) + "\n- " + context.i18n.text("select.style", style));
    }

    private void showSupport(SupportCharacterDefinition support) {
        showTexture(support.glyphTexture);

        String skillDescriptionKey = support.skill.descriptionKey != null ? support.skill.descriptionKey : "select.support_skill_detail." + supportOwner(support.skill.id, true);
        String ultimateDescriptionKey = support.ultimate.descriptionKey != null ? support.ultimate.descriptionKey : "select.support_ultimate_detail." + supportOwner(support.ultimate.id, false);
        String skill = context.i18n.text("guide.support_skill", context.i18n.text(support.skill.nameKey), decimal(cooldown(support.skill.cooldownSeconds)), decimal(support.skill.durationSeconds), Math.round(support.skill.magnitude * 100));
        String ultimate = context.i18n.text("guide.support_ultimate", context.i18n.text(support.ultimate.nameKey), Math.round(support.ultimate.pointCost));
        String style = support.custom ? context.i18n.text("select.custom_style") : context.i18n.text(support.styleKey);

        details.setText("- " + skill + "\n  " + context.i18n.text(skillDescriptionKey) + "\n- " + ultimate + "\n  " + context.i18n.text(ultimateDescriptionKey) + "\n- " + context.i18n.text("guide.field_passives_title") + "\n  " + fieldDetails(support) + "\n- " + context.i18n.text("select.style", style));
    }

    private String fieldDetails(SupportCharacterDefinition support) {
        List<String> descriptions = new ArrayList<>(Arrays.stream(support.fieldPassiveIds).map(id -> context.i18n.text("field." + id) + ": " + context.i18n.text("field.description." + id)).toList());

        if (support.zeroDefenseMain) {
            descriptions.add(context.i18n.text("support.passive.zero_defense_main"));
        }

        return String.join("\n  ", descriptions);
    }

    private String skillDetail(AbilityConfigs.ProjectileSkill skill) {
        return AbilityPresentationFormatter.format(context.registries.skillImplementations.require(skill.implementationId).presentation().apply(skill, context.config.battle.skillCooldownMultiplier), context.i18n);
    }

    private String ultimateDetail(AbilityConfigs.AreaUltimate ultimate) {
        return AbilityPresentationFormatter.format(context.registries.ultimateImplementations.require(ultimate.implementationId).presentation().apply(ultimate), context.i18n);
    }

    private void confirmCandidate() {
        if (pendingId == null) {
            return;
        }

        if (selectingMain()) {
            mainIds.add(pendingId);
        } else {
            supportIds.add(pendingId);
        }

        phase++;

        if (phase == playerCount + playerCount * supportCount) {
            startBattle();
            return;
        }

        updatePrompt();
        refreshCards();
    }

    private void clearCandidate() {
        if (selectedCard != null) {
            selectedCard.setBackground("button");
        }

        selectedCard = null;
        pendingId = null;
        confirm.setDisabled(true);
        preview.setDrawable(null);
        details.setText(context.i18n.text("select.choose_card_hint"));

        refreshPlayerPreviews();
    }

    private boolean selectingMain() {
        return phase < playerCount;
    }

    private void setSupportCount(int value) {
        if (selectingMain()) {
            return;
        }

        supportCount = Math.max(1, Math.min(2, value));
        supportIds.clear();
        phase = playerCount;

        updatePrompt();
        refreshCards();
        refreshPlayerPreviews();
    }

    private void updatePrompt() {
        int supportSelection = Math.max(0, phase - playerCount);
        int player = selectingMain() ? phase + 1 : supportSelection / supportCount + 1;

        supportCountControls.setVisible(!selectingMain());

        if (selectingMain()) {
            prompt.setText(context.i18n.text("select.main_prompt", player));
        } else {
            prompt.setText(context.i18n.text("select.support_prompt_slot", player, supportSelection % supportCount + 1, supportCount));
        }

        chosen.setText(context.i18n.text("select.selection_progress", Math.min(phase, playerCount + playerCount * supportCount), playerCount + playerCount * supportCount));
    }

    private void startBattle() {
        screens.show(new BattleScreen(context, screens, mode, List.copyOf(mainIds), List.copyOf(supportIds), supportCount));
    }

    private void refreshPlayerPreviews() {
        playerPreviews.clearChildren();

        boolean compact = playerCount > 2 && supportCount > 1;
        float mainSize = compact ? 42 : playerCount > 2 ? 48 : 74;
        float supportSize = compact ? 28 : playerCount > 2 ? 32 : 46;
        float nameWidth = compact ? 48 : playerCount > 2 ? 72 : 125;

        for (int player = 0; player < playerCount; player++) {
            Table card = new Table(context.skin);
            card.setBackground("panel");
            card.pad(6);

            Label playerLabel = new Label((player + 1) + "P", context.skin);
            playerLabel.setFontScale(.72f);
            card.add(playerLabel).left().row();

            Table portraits = new Table();
            addMainPreview(portraits, player, mainSize, nameWidth);

            for (int slot = 0; slot < supportCount; slot++) {
                addSupportPreview(portraits, player, slot, supportSize, nameWidth);
            }
            card.add(portraits);
            playerPreviews.add(card).expandX().fillX().height(120).pad(3);
        }
    }

    private void addMainPreview(Table portraits, int player, float imageSize, float nameWidth) {
        String mainId = player < mainIds.size() ? mainIds.get(player) : selectingMain() && player == phase ? pendingId : null;

        if (mainId == null) {
            portraits.add(new Label(context.i18n.text("select.unselected"), context.skin)).width(imageSize + nameWidth);
            return;
        }

        CharacterDefinition main = context.characters.require(mainId);
        portraits.add(new Image(drawable(main.glyphTexture))).size(imageSize, imageSize);

        Label label = new Label(name(main), context.skin);
        label.setWrap(true);
        label.setFontScale(playerCount > 2 && supportCount > 1 ? .44f : playerCount > 2 ? .52f : .7f);

        portraits.add(label).width(nameWidth).left();
    }

    private void addSupportPreview(Table portraits, int player, int slot, float imageSize, float nameWidth) {
        int supportSelection = phase - playerCount;
        int index = player * supportCount + slot;

        String supportId = index < supportIds.size() ? supportIds.get(index) : !selectingMain() && index == supportSelection ? pendingId : null;
        if (supportId == null) {
            return;
        }

        SupportCharacterDefinition support = context.supports.require(supportId);
        portraits.add(new Image(drawable(support.glyphTexture))).size(imageSize, imageSize).padLeft(playerCount > 2 && supportCount > 1 ? 2 : 6);

        Label label = new Label(name(support), context.skin);
        label.setWrap(true);
        label.setFontScale(playerCount > 2 && supportCount > 1 ? .4f : playerCount > 2 ? .48f : .65f);

        portraits.add(label).width(nameWidth).left();
    }

    private String name(CharacterDefinition value) {
        return value.displayName != null ? value.displayName : context.i18n.text(value.nameKey);
    }

    private String name(SupportCharacterDefinition value) {
        return value.displayName != null ? value.displayName : context.i18n.text(value.nameKey);
    }

    private String passiveNames(CharacterDefinition value) {
        return String.join(" / ", Arrays.stream(value.equippedPassiveIds()).map(id -> context.i18n.text(context.config.passive(id).nameKey)).toList());
    }

    private String passiveDescriptions(CharacterDefinition value) {
        return String.join("\n  ", Arrays.stream(value.equippedPassiveIds()).map(id -> context.i18n.text(context.config.passive(id).descriptionKey)).toList());
    }

    private String supportOwner(String abilityId, boolean skill) {
        return context.config.supports().stream().filter(value -> abilityId.equals(skill ? value.skill.id : value.ultimate.id)).map(value -> value.id).findFirst().orElse("t");
    }

    private static String decimal(float value) {
        return String.format("%.1f", value);
    }

    private float cooldown(float value) {
        return value * context.config.battle.skillCooldownMultiplier;
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Texture texture : previewTextures.values()) {
            texture.dispose();
        }
    }
}
