package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.registry.AbilityPresentationFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * メイン・補助キャラクターの能力値と能力説明を順番に閲覧するガイド画面。
 */
public final class CharacterGuideScreen extends StageScreen {
    private static final float GUIDE_WIDTH_RATIO = .97f;
    private static final float GUIDE_HEIGHT_RATIO = .95f;
    private static final String SECTION_GAP = "\n\n\n";
    private static final String HEADING_GAP = "\n\n";
    private final List<CharacterDefinition> characters;
    private final List<SupportCharacterDefinition> supports;
    private final Label heading;
    private final Label details;
    private final Table detailsContent = new Table();
    private final ScrollPane detailsScroll;
    private int index;

    public CharacterGuideScreen(GameContext context, ScreenManager screens) {
        super(context, screens);
        characters = new ArrayList<>(context.characters.all().stream().filter(value -> !value.custom).toList());
        supports = new ArrayList<>(context.supports.all().stream().filter(value -> !value.custom).toList());

        Table root = new Table();
        Table panel = new Table(context.skin);
        panel.setBackground("panel");
        panel.pad(18);

        heading = new Label("", context.skin);
        heading.setFontScale(1.45f);
        heading.setAlignment(Align.center);
        details = new Label("", context.skin);
        details.setWrap(true);
        details.setFontScale(.76f);
        details.setAlignment(Align.left, Align.top);

        detailsContent.top().left().pad(22, 26, 32, 26);
        detailsContent.add(details).growX().top().left();
        detailsScroll = new ScrollPane(detailsContent);
        detailsScroll.setFadeScrollBars(false);
        detailsScroll.setScrollingDisabled(true, false);
        detailsScroll.setOverscroll(false, false);

        panel.add(heading).growX().height(68).padBottom(12).row();
        panel.add(detailsScroll).grow().minWidth(0).minHeight(0).padBottom(12).row();

        TextButton previous = new TextButton(context.i18n.text("guide.previous"), context.skin);
        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        TextButton next = new TextButton(context.i18n.text("guide.next"), context.skin);

        Table navigation = new Table();

        navigation.add(previous).width(260).height(54).pad(8);
        navigation.add(back).width(260).height(54).pad(8);
        navigation.add(next).width(260).height(54).pad(8);

        panel.add(navigation).growX().height(70);

        previous.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                index = Math.floorMod(index - 1, totalEntries());
                refresh();
            }
        });

        next.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                index = (index + 1) % totalEntries();
                refresh();
            }
        });

        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new ModeSelectScreen(context, screens));
            }
        });

        addMenu(root, panel, GUIDE_WIDTH_RATIO, GUIDE_HEIGHT_RATIO);
        refresh();
    }

    private void refresh() {
        if (index >= characters.size()) {
            refreshSupport(supports.get(index - characters.size()));
            return;
        }

        CharacterDefinition character = characters.get(index);

        AbilityConfigs.ProjectileSkill skill = context.config.skill(character.skillId);
        AbilityConfigs.AreaUltimate ultimate = context.config.ultimate(character.ultimateId);

        heading.setText((character.uiLabel == null ? character.displayGlyph : character.uiLabel) + "  " + context.i18n.text(character.nameKey) + "  " + (index + 1) + "/" + totalEntries());

        String skillPower = AbilityPresentationFormatter.format(context.registries.skillImplementations.require(skill.implementationId).presentation().apply(jp.takoa.mojibattle.domain.ability.adapter.MapConfigView.forPresentation(skill), context.config.battle.skillCooldownMultiplier), context.i18n);
        String ultimatePower = AbilityPresentationFormatter.format(context.registries.ultimateImplementations.require(ultimate.implementationId).presentation().apply(jp.takoa.mojibattle.domain.ability.adapter.MapConfigView.forPresentation(ultimate)), context.i18n);

        AbilityConfigs.TimedPassive passive = context.config.passive(character.passiveId);

        String passiveStats = context.i18n.text(passive.descriptionKey);

        details.setText(String.join(SECTION_GAP, context.i18n.text("guide.character." + character.id), context.i18n.text("guide.stats", Math.round(character.maxHp), decimal(character.moveSpeed)), context.i18n.text("guide.skill", context.i18n.text(skill.nameKey)) + HEADING_GAP + skillPower, context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey)) + HEADING_GAP + ultimatePower + "\n" + context.i18n.text("guide.cost", Math.round(ultimate.pointCost)), context.i18n.text("guide.passive", context.i18n.text(passive.nameKey)) + HEADING_GAP + passiveStats));

        resetDetailsScroll();
    }

    private void refreshSupport(SupportCharacterDefinition support) {
        heading.setText(support.displayGlyph + "  " + context.i18n.text(support.nameKey) + "  " + (index + 1) + "/" + totalEntries());
        details.setText(String.join(SECTION_GAP, context.i18n.text("guide.support_role"), context.i18n.text("guide.support_stats", decimal(support.moveSpeed), decimal(support.followDistance), decimal(support.attackInterval), Math.round(support.baseDamage), decimal(support.attackRange)), context.i18n.text("guide.support_skill", context.i18n.text(support.skill.nameKey), decimal(cooldown(support.skill.cooldownSeconds)), decimal(support.skill.durationSeconds), Math.round(support.skill.magnitude * 100)) + HEADING_GAP + context.i18n.text(support.skill.descriptionKey), context.i18n.text("guide.support_ultimate", context.i18n.text(support.ultimate.nameKey), Math.round(support.ultimate.pointCost)) + HEADING_GAP + context.i18n.text(support.ultimate.descriptionKey), context.i18n.text("guide.field_passives_title") + HEADING_GAP + fieldDetails(support), context.i18n.text(support.styleKey)));

        resetDetailsScroll();
    }

    private void resetDetailsScroll() {
        details.invalidateHierarchy();
        detailsContent.invalidateHierarchy();
        detailsScroll.setScrollY(0);
    }

    private String fieldDetails(SupportCharacterDefinition support) {
        List<String> descriptions = new ArrayList<>(java.util.Arrays.stream(support.fieldPassiveIds).map(id -> context.i18n.text("field." + id) + ": " + context.i18n.text("field.description." + id)).toList());

        if (support.zeroDefenseMain) {
            descriptions.add(context.i18n.text("support.passive.zero_defense_main"));
        }

        return String.join(HEADING_GAP, descriptions);
    }

    private int totalEntries() {
        return characters.size() + supports.size();
    }

    private static String decimal(float value) {
        return String.format("%.1f", value);
    }

    private float cooldown(float value) {
        return value * context.config.battle.skillCooldownMultiplier;
    }
}
