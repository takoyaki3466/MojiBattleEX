package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
import jp.takoa.mojibattle.log.DiagnosticLog;
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
    private static final float MENU_WIDTH_RATIO = .997f;
    private static final float MENU_HEIGHT_RATIO = .992f;
    private static final int CARD_COLUMNS = 7;
    private static final float CARD_WIDTH = 104f;
    private static final float CARD_HEIGHT = 92f;

    private final BattleMode mode;
    private final int playerCount;
    private final int mainCount;
    private final GdxTextureLoader textureLoader;
    private final Label prompt;
    private final Label chosen;
    private final Table detailsContent = new Table();

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
        mainCount = mode == BattleMode.CHAOS ? 3 : 1;
        supportCount = mode == BattleMode.CHAOS ? 4 : 1;

        Table root = new Table();
        root.setFillParent(true);
        root.pad(0);
        Table roster = new Table(context.skin);
        roster.setBackground("panel");
        roster.pad(7);

        Label heading = new Label(context.i18n.text("select.title"), context.skin, "title");
        heading.setFontScale(.48f);
        roster.add(heading).growX().height(52).left().row();

        prompt = new Label("", context.skin);
        prompt.setColor(.62f, 1f, .76f, 1f);
        roster.add(prompt).left().row();

        chosen = new Label("", context.skin);
        chosen.setFontScale(.62f);
        roster.add(chosen).left().padBottom(4).row();

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

        tabs.add(existingTab).width(230).height(44).padRight(4);
        tabs.add(customTab).width(230).height(44).padLeft(4);

        roster.add(tabs).left().padBottom(4).row();

        cardGrid.top().left();
        ScrollPane cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFadeScrollBars(false);
        cardScroll.setOverscroll(false, false);
        cardScroll.setScrollingDisabled(false, false);
        roster.add(cardScroll).grow().minWidth(0).minHeight(0).top().left();

        Table information = new Table(context.skin);
        information.setBackground("panel");
        information.pad(9);

        preview.setScaling(Scaling.fit);
        information.add(preview).growX().height(158).row();

        detailsContent.top().left();
        ScrollPane detailsScroll = new ScrollPane(detailsContent);
        detailsScroll.setFadeScrollBars(false);
        detailsScroll.setOverscroll(false, false);
        detailsScroll.setScrollingDisabled(true, false);
        information.add(detailsScroll).grow().minWidth(0).minHeight(0).top().left().padTop(5).row();
        showDetailHint();

        Table decisions = new Table();
        cancel = new TextButton(context.i18n.text("select.cancel"), context.skin);
        confirm = new TextButton(context.i18n.text("select.confirm"), context.skin);

        confirm.setDisabled(true);

        decisions.add(cancel).growX().height(46).padRight(4);
        decisions.add(confirm).growX().height(46).padLeft(4);

        information.add(decisions).growX().height(52).padTop(4);

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
        upper.add(roster).grow().minWidth(0).minHeight(0).top().padRight(3);
        upper.add(information).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.31f, upper))
                .growY().minWidth(330).minHeight(0).top().padLeft(3);

        Table shell = new Table(context.skin);
        shell.setBackground("bar-bg");
        shell.pad(3);
        shell.add(upper).grow().minWidth(0).minHeight(0).top().row();
        shell.add(playerPreviews).growX().height(112).padTop(3);

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

        showDetailHint();
        existingTab.setText((showingCustom ? "" : "▶ ") + context.i18n.text("select.existing"));
        customTab.setText((showingCustom ? "▶ " : "") + context.i18n.text("select.custom"));

        int index = selectingMain() ? addMainCards() : addSupportCards();

        if (index == 0) {
            Label empty = new Label(context.i18n.text("select.custom_empty"), context.skin);
            empty.setWrap(true);
            cardGrid.add(empty).width(650).height(120).center();
        }
        refreshPlayerPreviews();
        updateDiagnostics();
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

        card.add(image).size(45, 45).row();
        card.add(glyphLabel).width(90).height(18).row();
        card.add(summaryLabel).width(90).height(22).top();

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
                updateDiagnostics();
                detailsAction.run();

                refreshPlayerPreviews();
                return true;
            }
        });

        cardGrid.add(card).width(CARD_WIDTH).height(CARD_HEIGHT).pad(3).top();
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

        String style = context.i18n.text(styleDescriptionKey(character, skill));
        showDetailCards(
                name(character),
                new DetailSection(
                        context.i18n.text("guide.skill", context.i18n.text(skill.nameKey)),
                        skillDetail(skill),
                        new Color(.20f, .88f, 1f, 1f)),
                new DetailSection(
                        context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey)),
                        ultimateDetail(ultimate) + "\n" + context.i18n.text("guide.cost", Math.round(ultimate.pointCost)),
                        new Color(1f, .72f, .12f, 1f)),
                new DetailSection(
                        context.i18n.text("guide.passive", passiveNames(character)),
                        passiveDescriptions(character),
                        new Color(.55f, 1f, .34f, 1f)),
                new DetailSection(
                        context.i18n.text("select.style", ""),
                        style,
                        new Color(1f, .50f, .78f, 1f)));
    }

    private void showSupport(SupportCharacterDefinition support) {
        showTexture(support.glyphTexture);

        String skillDescriptionKey = support.skill.descriptionKey != null ? support.skill.descriptionKey : "select.support_skill_detail." + supportOwner(support.skill.id, true);
        String ultimateDescriptionKey = support.ultimate.descriptionKey != null ? support.ultimate.descriptionKey : "select.support_ultimate_detail." + supportOwner(support.ultimate.id, false);
        String skill = context.i18n.text("guide.support_skill", context.i18n.text(support.skill.nameKey), decimal(cooldown(support.skill.cooldownSeconds)), decimal(support.skill.durationSeconds), Math.round(support.skill.magnitude * 100));
        String ultimate = context.i18n.text("guide.support_ultimate", context.i18n.text(support.ultimate.nameKey), Math.round(support.ultimate.pointCost));
        String style = support.custom ? context.i18n.text("select.custom_style") : context.i18n.text(support.styleKey);

        showDetailCards(
                name(support),
                new DetailSection(skill, context.i18n.text(skillDescriptionKey), new Color(.20f, .88f, 1f, 1f)),
                new DetailSection(ultimate, context.i18n.text(ultimateDescriptionKey), new Color(1f, .72f, .12f, 1f)),
                new DetailSection(context.i18n.text("guide.field_passives_title"), fieldDetails(support),
                        new Color(.55f, 1f, .34f, 1f)),
                new DetailSection(context.i18n.text("select.style", ""), style,
                        new Color(1f, .50f, .78f, 1f)));
    }

    /** 右側の解説を、強調見出しと本文を持つ独立した枠へ組み直す。 */
    private void showDetailCards(String title, DetailSection... sections) {
        detailsContent.clearChildren();

        Label titleLabel = new Label(title, context.skin, "title");
        titleLabel.setFontScale(.34f);
        titleLabel.setAlignment(Align.center);
        detailsContent.add(titleLabel).growX().height(54).pad(4).row();

        for (DetailSection section : sections) {
            Table card = new Table(context.skin);
            card.setBackground("bar-bg");
            card.pad(9);

            Label heading = new Label(section.heading(), context.skin, "effect-text");
            heading.setWrap(true);
            heading.setFontScale(.72f);
            heading.setColor(section.accent());
            heading.setAlignment(Align.left);

            Label body = new Label(section.body(), context.skin);
            body.setWrap(true);
            body.setFontScale(.66f);
            body.setAlignment(Align.left, Align.top);

            card.add(heading).growX().width(310).left().padBottom(7).row();
            card.add(body).growX().width(310).left().top();
            detailsContent.add(card).growX().minWidth(300).maxWidth(420).pad(4, 3, 5, 3).row();
        }
    }

    private void showDetailHint() {
        detailsContent.clearChildren();
        Label hint = new Label(context.i18n.text("select.choose_card_hint"), context.skin, "effect-text");
        hint.setWrap(true);
        hint.setFontScale(.72f);
        hint.setAlignment(Align.center);
        detailsContent.add(hint).width(310).growY().center().pad(24);
    }

    private record DetailSection(String heading, String body, Color accent) {
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

        pendingId = null;
        phase++;
        updateDiagnostics();

        if (phase == totalSelections()) {
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
        showDetailHint();

        refreshPlayerPreviews();
        updateDiagnostics();
    }

    private boolean selectingMain() {
        return phase < mainSelectionCount();
    }

    private void setSupportCount(int value) {
        if (selectingMain()) {
            return;
        }

        supportCount = Math.max(1, Math.min(2, value));
        supportIds.clear();
        phase = mainSelectionCount();

        updatePrompt();
        refreshCards();
        refreshPlayerPreviews();
    }

    private void updatePrompt() {
        int supportSelection = Math.max(0, phase - mainSelectionCount());
        int player = selectingMain() ? phase / mainCount + 1 : supportSelection / supportCount + 1;

        supportCountControls.setVisible(mode != BattleMode.CHAOS && !selectingMain());

        if (selectingMain()) {
            if (mode == BattleMode.CHAOS) {
                prompt.setText(context.i18n.text(
                        "select.chaos_main_prompt",
                        player,
                        phase % mainCount + 1,
                        mainCount));
            } else {
                prompt.setText(context.i18n.text("select.main_prompt", player));
            }
        } else {
            prompt.setText(context.i18n.text("select.support_prompt_slot", player, supportSelection % supportCount + 1, supportCount));
        }

        chosen.setText(context.i18n.text("select.selection_progress", Math.min(phase, totalSelections()), totalSelections()));
    }

    private void startBattle() {
        try {
            screens.show(new BattleScreen(
                    context,
                    screens,
                    mode,
                    List.copyOf(mainIds),
                    List.copyOf(supportIds),
                    supportCount));
        } catch (RuntimeException | Error error) {
            DiagnosticLog.error("Battle screen transition failed", error);
            rollbackLastSelection();
            showBattleStartError();
        }
    }

    private void rollbackLastSelection() {
        if (!supportIds.isEmpty()) {
            supportIds.removeLast();
        } else if (!mainIds.isEmpty()) {
            mainIds.removeLast();
        }
        phase = Math.max(0, phase - 1);
        pendingId = null;
        updatePrompt();
        refreshCards();
        updateDiagnostics();
    }

    private void showBattleStartError() {
        Dialog dialog = new Dialog(context.i18n.text("battle.start.error.title"), context.skin);
        Label message = new Label(
                context.i18n.text("battle.start.error.message", DiagnosticLog.file()),
                context.skin);
        message.setWrap(true);
        dialog.getContentTable().add(message).width(720).pad(24);
        dialog.button(context.i18n.text("battle.start.error.dismiss"));
        dialog.show(stage);
    }

    private void updateDiagnostics() {
        List<String> effectiveMainIds = new ArrayList<>(mainIds);
        List<String> effectiveSupportIds = new ArrayList<>(supportIds);
        if (pendingId != null) {
            (selectingMain() ? effectiveMainIds : effectiveSupportIds).add(pendingId);
        }

        List<String> glyphTextures = new ArrayList<>();
        for (String id : effectiveMainIds) {
            glyphTextures.add(id + "=" + context.characters.require(id).glyphTexture);
        }
        List<String> projectileTextures = new ArrayList<>();
        for (String id : effectiveSupportIds) {
            SupportCharacterDefinition support = context.supports.require(id);
            glyphTextures.add(id + "=" + support.glyphTexture);
            projectileTextures.add(id + "=" + support.projectileTexture);
        }
        DiagnosticLog.setSelection(effectiveMainIds, effectiveSupportIds, glyphTextures, projectileTextures);
    }

    private void refreshPlayerPreviews() {
        playerPreviews.clearChildren();

        boolean denseChaos = mode == BattleMode.CHAOS && playerCount > 2;
        boolean compact = mode == BattleMode.CHAOS || playerCount > 2 && supportCount > 1;
        float mainSize = denseChaos ? 34 : compact ? 42 : playerCount > 2 ? 48 : 66;
        float supportSize = denseChaos ? 28 : compact ? 34 : playerCount > 2 ? 40 : 52;

        for (int player = 0; player < playerCount; player++) {
            Table card = new Table(context.skin);
            card.setBackground("panel");
            card.pad(4);

            Label playerLabel = new Label((player + 1) + "P", context.skin);
            playerLabel.setFontScale(.66f);
            card.add(playerLabel).left().height(20).row();

            Table portraits = new Table();
            for (int slot = 0; slot < mainCount; slot++) {
                addMainPreview(portraits, player, slot, mainSize);
            }
            for (int slot = 0; slot < supportCount; slot++) {
                addSupportPreview(portraits, player, slot, supportSize);
            }
            card.add(portraits).expand().center();
            playerPreviews.add(card).expandX().fillX().height(106).pad(2);
        }
    }

    private void addMainPreview(Table portraits, int player, int slot, float imageSize) {
        int index = player * mainCount + slot;
        String mainId = index < mainIds.size()
                ? mainIds.get(index)
                : selectingMain() && index == phase ? pendingId : null;

        if (mainId == null) {
            portraits.add(new Label(context.i18n.text("select.unselected"), context.skin))
                    .width(imageSize).height(imageSize).center().pad(2);
            return;
        }

        CharacterDefinition main = context.characters.require(mainId);
        portraits.add(new Image(drawable(main.glyphTexture))).size(imageSize, imageSize).pad(2);
    }

    private void addSupportPreview(Table portraits, int player, int slot, float imageSize) {
        int supportSelection = phase - mainSelectionCount();
        int index = player * supportCount + slot;

        String supportId = index < supportIds.size() ? supportIds.get(index) : !selectingMain() && index == supportSelection ? pendingId : null;
        if (supportId == null) {
            return;
        }

        SupportCharacterDefinition support = context.supports.require(supportId);
        portraits.add(new Image(drawable(support.glyphTexture))).size(imageSize, imageSize).pad(2);
    }

    private String name(CharacterDefinition value) {
        return value.displayName != null ? value.displayName : context.i18n.text(value.nameKey);
    }

    private int mainSelectionCount() {
        return playerCount * mainCount;
    }

    private int totalSelections() {
        return mainSelectionCount() + playerCount * supportCount;
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

    static String styleDescriptionKey(CharacterDefinition character, AbilityConfigs.ProjectileSkill skill) {
        return character.custom ? "select.custom_style" : skill.descriptionKey;
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
