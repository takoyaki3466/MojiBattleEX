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

import java.util.ArrayList;
import java.util.List;

/** メイン・補助キャラクターの能力値と能力説明を順番に閲覧するガイド画面。 */
public final class CharacterGuideScreen extends StageScreen {
    private static final float SKILL_DAMAGE_SCALE = .55f;
    private static final float ULTIMATE_DAMAGE_SCALE = .65f;
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
        characters = new ArrayList<>(context.characters.all().stream()
                .filter(value -> !value.custom)
                .toList());
        supports = new ArrayList<>(context.supports.all().stream()
                .filter(value -> !value.custom)
                .toList());

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
        previous.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { index = Math.floorMod(index - 1, totalEntries()); refresh(); }});
        next.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { index = (index + 1) % totalEntries(); refresh(); }});
        back.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { screens.show(new ModeSelectScreen(context, screens)); }});
        addMenu(root, panel, GUIDE_WIDTH_RATIO, GUIDE_HEIGHT_RATIO);
        refresh();
    }

    private void refresh() {
        if(index>=characters.size()){refreshSupport(supports.get(index-characters.size()));return;}
        CharacterDefinition character = characters.get(index);
        AbilityConfigs.ProjectileSkill skill = context.config.skill(character.skillId);
        AbilityConfigs.AreaUltimate ultimate = context.config.ultimate(character.ultimateId);
        heading.setText((character.uiLabel==null?character.displayGlyph:character.uiLabel) + "  " + context.i18n.text(character.nameKey) + "  " + (index + 1) + "/" + totalEntries());
        int projectileCount="bind_follow_up".equals(skill.type)?1:skill.glyphs.length;
        float points = skill.ultimatePointsPerHit * (skill.pointsOnHit?1:projectileCount);
        String stun = skill.stunSeconds > 0 ? context.i18n.text("guide.stun", decimal(skill.stunSeconds)) : context.i18n.text("guide.no_stun");
        String ultimatePower = switch (ultimate.type) {
            case "self_heal" -> context.i18n.text("guide.heal", Math.round(ultimate.healAmount));
            case "line" -> context.i18n.text("guide.line", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE), decimal(ultimate.lineLength),
                decimal(ultimate.lineWidth * 5.2f), Math.max(1, ultimate.directions));
            case "meteor_shower" -> context.i18n.text("guide.meteor", ultimate.meteorCount, Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE), decimal(ultimate.radius * 5f));
            case "summon_family" -> context.i18n.text("guide.family_ultimate", ultimate.summonCount,
                    Math.round(ultimate.summonHp), decimal(ultimate.summonAttackDamage), decimal(ultimate.summonDefense));
            case "moving_car" -> context.i18n.text("guide.car", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                decimal(ultimate.effectSeconds), decimal(ultimate.stunSeconds));
            case "spinning_one" -> context.i18n.text("guide.spinning_one", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                decimal(ultimate.lineLength), Math.round(ultimate.healAmount));
            case "skill_barrage" -> context.i18n.text("guide.barrage",ultimate.barrageCount,decimal(ultimate.barrageInterval));
            case "sushi_barrage" -> context.i18n.text("guide.sushi_barrage",ultimate.barrageCount,Math.round(ultimate.baseDamage),decimal(ultimate.barrageInterval),decimal(ultimate.projectileSpeed));
            case "cash_crash" -> context.i18n.text("guide.cash_crash", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                    decimal(ultimate.radius * 5f), Math.round((ultimate.defenseDownMultiplier - 1f) * 100),
                    decimal(ultimate.defenseDownDuration));
            default -> context.i18n.text("guide.area", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE), decimal(ultimate.radius * 5f));
        };
        String passiveStats = context.i18n.text("guide.passive_effect." + character.id);
        String skillPower="random_economy".equals(skill.type)
            ?context.i18n.text("guide.random_economy",decimal(cooldown(skill.cooldownSeconds)),Math.round(points))
            :"self_attack_buff".equals(skill.type)
            ?context.i18n.text("guide.attack_buff",decimal(cooldown(skill.cooldownSeconds)),Math.round((skill.buffAttackMultiplier-1)*100),decimal(skill.buffDurationSeconds))
            :"bind_follow_up".equals(skill.type)
            ?context.i18n.text("guide.bind_follow_up",decimal(cooldown(skill.cooldownSeconds)),Math.round(skill.damages[0]*SKILL_DAMAGE_SCALE),decimal(skill.bindDurations[0]),Math.round(skill.followUpDamage*SKILL_DAMAGE_SCALE),Math.round(points))
            :context.i18n.text("guide.skill_stats", decimal(cooldown(skill.cooldownSeconds)), Math.round(skill.baseDamage * SKILL_DAMAGE_SCALE), skill.glyphs.length,
                decimal(Math.max(3.2f, skill.projectileRadius * 5f) * 2f), Math.round(points), stun);
        if (skill.fallingObjectTexture != null) {
            skillPower += "\n" + context.i18n.text(
                    "guide.falling_skill",
                    Math.round(skill.fallingDamage * SKILL_DAMAGE_SCALE),
                    decimal(skill.fallingRadius * 5f));
        }
        if(skill.healthCostRatio>0)skillPower+=" / "+context.i18n.text("guide.health_cost",Math.round(skill.healthCostRatio*100));
        details.setText(String.join(
                SECTION_GAP,
                context.i18n.text("guide.character." + character.id),
                context.i18n.text(
                        "guide.stats",
                        Math.round(character.maxHp),
                        decimal(character.moveSpeed)),
                context.i18n.text(
                        "guide.skill",
                        context.i18n.text("ability." + character.id + ".skill.name"))
                        + HEADING_GAP
                        + skillPower,
                context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey))
                        + HEADING_GAP
                        + ultimatePower
                        + "\n"
                        + context.i18n.text("guide.cost", Math.round(ultimate.pointCost)),
                context.i18n.text(
                        "guide.passive",
                        context.i18n.text("ability." + character.id + ".passive.name"))
                        + HEADING_GAP
                        + passiveStats));
        resetDetailsScroll();
    }

    private void refreshSupport(SupportCharacterDefinition support) {
        heading.setText(
                support.displayGlyph
                        + "  "
                        + context.i18n.text(support.nameKey)
                        + "  "
                        + (index + 1)
                        + "/"
                        + totalEntries());
        details.setText(String.join(
                SECTION_GAP,
                context.i18n.text("guide.support_role"),
                context.i18n.text(
                        "guide.support_stats",
                        decimal(support.moveSpeed),
                        decimal(support.followDistance),
                        decimal(support.attackInterval),
                        Math.round(support.baseDamage),
                        decimal(support.attackRange)),
                context.i18n.text(
                        "guide.support_skill",
                        context.i18n.text(support.skill.nameKey),
                        decimal(cooldown(support.skill.cooldownSeconds)),
                        decimal(support.skill.durationSeconds),
                        Math.round(support.skill.magnitude * 100))
                        + HEADING_GAP
                        + context.i18n.text("select.support_skill_detail." + support.id),
                context.i18n.text(
                        "guide.support_ultimate",
                        context.i18n.text(support.ultimate.nameKey),
                        Math.round(support.ultimate.pointCost))
                        + HEADING_GAP
                        + context.i18n.text("select.support_ultimate_detail." + support.id),
                context.i18n.text("guide.field_passives_title")
                        + HEADING_GAP
                        + fieldDetails(support),
                context.i18n.text("select.support_style." + support.id)));
        resetDetailsScroll();
    }

    private void resetDetailsScroll() {
        details.invalidateHierarchy();
        detailsContent.invalidateHierarchy();
        detailsScroll.setScrollY(0);
    }

    private String fieldDetails(SupportCharacterDefinition support) {
        List<String> descriptions = new ArrayList<>(
                java.util.Arrays.stream(support.fieldPassiveIds)
                        .map(id -> context.i18n.text("field." + id)
                                + ": "
                                + context.i18n.text("field.description." + id))
                        .toList());
        if (support.zeroDefenseMain) {
            descriptions.add(context.i18n.text("support.passive.zero_defense_main"));
        }
        return String.join(HEADING_GAP, descriptions);
    }

    private int totalEntries() {
        return characters.size() + supports.size();
    }

    private static String decimal(float value) { return String.format("%.1f", value); }
    private float cooldown(float value){return value*context.config.battle.skillCooldownMultiplier;}
}
