package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** プレイヤーごとにメインと補助キャラクターを選び、詳細確認後に戦闘を開始する画面。 */
public final class CharacterSelectScreen extends StageScreen {
    private static final float SKILL_DAMAGE_SCALE = .55f;
    private static final float ULTIMATE_DAMAGE_SCALE = .65f;
    private static final float MENU_WIDTH_RATIO = .985f;
    private static final float MENU_HEIGHT_RATIO = .97f;
    private static final int CARD_COLUMNS = 5;
    private final BattleMode mode;
    private final int playerCount;
    private final Label prompt;
    private final Label chosen;
    private final Label details;
    private final Image preview = new Image();
    private final Table cardGrid = new Table();
    private final Table playerPreviews = new Table();
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
    private boolean showingCustom;

    public CharacterSelectScreen(
            GameContext context,
            ScreenManager screens,
            BattleMode mode,
            int playerCount) {
        super(context, screens);
        this.mode = mode;
        this.playerCount = mode == BattleMode.TRAINING
                ? 1
                : Math.max(2, Math.min(4, playerCount));

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

        Table tabs = new Table();
        existingTab = new TextButton(context.i18n.text("select.existing"), context.skin);
        customTab = new TextButton(context.i18n.text("select.custom"), context.skin);
        tabs.add(existingTab).width(260).height(52).padRight(5);
        tabs.add(customTab).width(260).height(52).padLeft(5);
        roster.add(tabs).left().padBottom(5).row();
        roster.add(cardGrid).width(705).height(348).top().left();

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
            addCard(
                    character.id,
                    character.glyphTexture,
                    glyph,
                    name(character),
                    () -> showMain(character));
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
            addCard(
                    support.id,
                    support.glyphTexture,
                    support.displayGlyph,
                    name(support),
                    () -> showSupport(support));
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
            Texture created = new Texture(Gdx.files.internal(key), true);
            created.setFilter(
                    Texture.TextureFilter.MipMapLinearLinear,
                    Texture.TextureFilter.Linear);
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
        String style = character.custom
                ? context.i18n.text("select.custom_style")
                : context.i18n.text("guide.character." + character.id);
        details.setText(
                "- " + context.i18n.text("guide.skill", abilityName(character.skillId, "skill"))
                        + "\n  " + skillDetail(skill)
                        + "\n- " + context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey))
                        + "\n  " + ultimateDetail(ultimate)
                        + " / " + context.i18n.text("guide.cost", Math.round(ultimate.pointCost))
                        + "\n- " + context.i18n.text("guide.passive", passiveNames(character))
                        + "\n  " + passiveDescriptions(character)
                        + "\n- " + context.i18n.text("select.style", style));
    }

    private void showSupport(SupportCharacterDefinition support) {
        showTexture(support.glyphTexture);
        String skillOwner = supportOwner(support.skill.id, true);
        String ultimateOwner = supportOwner(support.ultimate.id, false);
        String skill = context.i18n.text(
                "guide.support_skill",
                context.i18n.text(support.skill.nameKey),
                decimal(cooldown(support.skill.cooldownSeconds)),
                decimal(support.skill.durationSeconds),
                Math.round(support.skill.magnitude * 100));
        String ultimate = context.i18n.text(
                "guide.support_ultimate",
                context.i18n.text(support.ultimate.nameKey),
                Math.round(support.ultimate.pointCost));
        String style = support.custom
                ? context.i18n.text("select.custom_style")
                : context.i18n.text("select.support_style." + support.id);
        details.setText(
                "- " + skill
                        + "\n  " + context.i18n.text("select.support_skill_detail." + skillOwner)
                        + "\n- " + ultimate
                        + "\n  " + context.i18n.text("select.support_ultimate_detail." + ultimateOwner)
                        + "\n- " + context.i18n.text("guide.field_passives_title")
                        + "\n  " + fieldDetails(support)
                        + "\n- " + context.i18n.text("select.style", style));
    }

    private String fieldDetails(SupportCharacterDefinition support) {
        return String.join(
                "\n  ",
                Arrays.stream(support.fieldPassiveIds)
                        .map(id -> context.i18n.text("field." + id)
                                + ": "
                                + context.i18n.text("field.description." + id))
                        .toList());
    }

    private String skillDetail(AbilityConfigs.ProjectileSkill skill) {
        int count = "bind_follow_up".equals(skill.type) ? 1 : skill.glyphs.length;
        float points = skill.ultimatePointsPerHit * (skill.pointsOnHit ? 1 : count);
        String health = skill.healthCostRatio > 0
                ? " / " + context.i18n.text("guide.health_cost", Math.round(skill.healthCostRatio * 100))
                : "";
        if ("self_attack_buff".equals(skill.type)) {
            return context.i18n.text(
                    "guide.attack_buff",
                    decimal(cooldown(skill.cooldownSeconds)),
                    Math.round((skill.buffAttackMultiplier - 1) * 100),
                    decimal(skill.buffDurationSeconds));
        }
        if ("bind_follow_up".equals(skill.type)) {
            return context.i18n.text(
                    "guide.bind_follow_up",
                    decimal(cooldown(skill.cooldownSeconds)),
                    Math.round(skill.damages[0] * SKILL_DAMAGE_SCALE),
                    decimal(skill.bindDurations[0]),
                    Math.round(skill.followUpDamage * SKILL_DAMAGE_SCALE),
                    Math.round(points)) + health;
        }
        String stun = skill.stunSeconds > 0
                ? context.i18n.text("guide.stun", decimal(skill.stunSeconds))
                : context.i18n.text("guide.no_stun");
        String result = context.i18n.text(
                "guide.skill_stats",
                decimal(cooldown(skill.cooldownSeconds)),
                Math.round(skill.baseDamage * SKILL_DAMAGE_SCALE),
                count,
                decimal(Math.max(3.2f, skill.projectileRadius * 5f) * 2f),
                Math.round(points),
                stun) + health;
        if (skill.fallingObjectTexture != null) {
            result += "\n" + context.i18n.text(
                    "guide.falling_skill",
                    Math.round(skill.fallingDamage * SKILL_DAMAGE_SCALE),
                    decimal(skill.fallingRadius * 5f));
        }
        return result;
    }

    private String ultimateDetail(AbilityConfigs.AreaUltimate ultimate) {
        return switch (ultimate.type) {
            case "self_heal" -> context.i18n.text("guide.heal", Math.round(ultimate.healAmount));
            case "line" -> context.i18n.text(
                    "guide.line",
                    Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.lineLength),
                    decimal(ultimate.lineWidth * 5.2f),
                    Math.max(1, ultimate.directions));
            case "meteor_shower" -> context.i18n.text(
                    "guide.meteor",
                    ultimate.meteorCount,
                    Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.radius * 5f));
            case "summon_family" -> context.i18n.text(
                    "guide.family_ultimate",
                    ultimate.summonCount,
                    Math.round(ultimate.summonHp),
                    decimal(ultimate.summonAttackDamage),
                    decimal(ultimate.summonDefense));
            case "moving_car" -> context.i18n.text(
                    "guide.car",
                    Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.effectSeconds),
                    decimal(ultimate.stunSeconds));
            case "spinning_one" -> context.i18n.text(
                    "guide.spinning_one",
                    Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.lineLength),
                    Math.round(ultimate.healAmount));
            case "skill_barrage" -> context.i18n.text(
                    "guide.barrage",
                    ultimate.barrageCount,
                    decimal(ultimate.barrageInterval));
            case "sushi_barrage" -> context.i18n.text(
                    "guide.sushi_barrage",
                    ultimate.barrageCount,
                    Math.round(ultimate.baseDamage),
                    decimal(ultimate.barrageInterval),
                    decimal(ultimate.projectileSpeed));
            default -> context.i18n.text(
                    "guide.area",
                    Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.radius * 5f));
        };
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
        if (phase == playerCount * 2) {
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

    private void updatePrompt() {
        int player = selectingMain() ? phase + 1 : phase - playerCount + 1;
        String promptKey = selectingMain() ? "select.main_prompt" : "select.support_prompt";
        prompt.setText(context.i18n.text(promptKey, player));
        chosen.setText(context.i18n.text(
                "select.selection_progress",
                Math.min(phase, playerCount * 2),
                playerCount * 2));
    }

    private void startBattle() {
        screens.show(new BattleScreen(
                context,
                screens,
                mode,
                List.copyOf(mainIds),
                List.copyOf(supportIds)));
    }
    private void refreshPlayerPreviews() {
        playerPreviews.clearChildren();
        float mainSize = playerCount > 2 ? 48 : 74;
        float supportSize = playerCount > 2 ? 32 : 46;
        float nameWidth = playerCount > 2 ? 72 : 125;
        for (int player = 0; player < playerCount; player++) {
            Table card = new Table(context.skin);
            card.setBackground("panel");
            card.pad(6);
            Label playerLabel = new Label((player + 1) + "P", context.skin);
            playerLabel.setFontScale(.72f);
            card.add(playerLabel).left().row();

            Table portraits = new Table();
            addMainPreview(portraits, player, mainSize, nameWidth);
            addSupportPreview(portraits, player, supportSize, nameWidth);
            card.add(portraits);
            playerPreviews.add(card).expandX().fillX().height(120).pad(3);
        }
    }

    private void addMainPreview(Table portraits, int player, float imageSize, float nameWidth) {
        String mainId = player < mainIds.size()
                ? mainIds.get(player)
                : selectingMain() && player == phase ? pendingId : null;
        if (mainId == null) {
            portraits.add(new Label(context.i18n.text("select.unselected"), context.skin))
                    .width(imageSize + nameWidth);
            return;
        }
        CharacterDefinition main = context.characters.require(mainId);
        portraits.add(new Image(drawable(main.glyphTexture))).size(imageSize, imageSize);
        Label label = new Label(name(main), context.skin);
        label.setWrap(true);
        label.setFontScale(playerCount > 2 ? .52f : .7f);
        portraits.add(label).width(nameWidth).left();
    }

    private void addSupportPreview(Table portraits, int player, float imageSize, float nameWidth) {
        int supportPlayer = phase - playerCount;
        String supportId = player < supportIds.size()
                ? supportIds.get(player)
                : !selectingMain() && player == supportPlayer ? pendingId : null;
        if (supportId == null) {
            return;
        }
        SupportCharacterDefinition support = context.supports.require(supportId);
        portraits.add(new Image(drawable(support.glyphTexture)))
                .size(imageSize, imageSize)
                .padLeft(6);
        Label label = new Label(name(support), context.skin);
        label.setWrap(true);
        label.setFontScale(playerCount > 2 ? .48f : .65f);
        portraits.add(label).width(nameWidth).left();
    }

    private String name(CharacterDefinition value) {
        return value.displayName != null ? value.displayName : context.i18n.text(value.nameKey);
    }

    private String name(SupportCharacterDefinition value) {
        return value.displayName != null ? value.displayName : context.i18n.text(value.nameKey);
    }

    private String abilityName(String id, String kind) {
        CharacterDefinition owner = context.config.characters().stream()
                .filter(value -> kind.equals("skill")
                        ? id.equals(value.skillId)
                        : Arrays.asList(value.equippedPassiveIds()).contains(id))
                .findFirst()
                .orElse(null);
        return owner == null
                ? id
                : context.i18n.text("ability." + owner.id + "." + kind + ".name");
    }

    private String passiveNames(CharacterDefinition value) {
        return String.join(
                " / ",
                Arrays.stream(value.equippedPassiveIds())
                        .map(id -> abilityName(id, "passive"))
                        .toList());
    }

    private String passiveDescriptions(CharacterDefinition value) {
        return String.join(
                "\n  ",
                Arrays.stream(value.equippedPassiveIds()).map(id -> {
                    CharacterDefinition owner = context.config.characters().stream()
                            .filter(candidate -> id.equals(candidate.passiveId))
                            .findFirst()
                            .orElse(null);
                    return owner == null
                            ? id
                            : context.i18n.text("guide.passive_effect." + owner.id);
                }).toList());
    }

    private String supportOwner(String abilityId, boolean skill) {
        return context.config.supports().stream()
                .filter(value -> abilityId.equals(skill ? value.skill.id : value.ultimate.id))
                .map(value -> value.id)
                .findFirst()
                .orElse("t");
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
