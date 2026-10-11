package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;

import java.util.ArrayList;
import java.util.List;

public final class CharacterGuideScreen extends StageScreen {
    private static final float SKILL_DAMAGE_SCALE = .55f;
    private static final float ULTIMATE_DAMAGE_SCALE = .65f;
    private final List<CharacterDefinition> characters;
    private final List<SupportCharacterDefinition> supports;
    private final Label heading;
    private final Label details;
    private int index;

    public CharacterGuideScreen(GameContext context, ScreenManager screens) {
        super(context, screens);
        characters = new ArrayList<>(context.characters.all());
        supports = new ArrayList<>(context.supports.all());
        Table root = new Table(); root.setFillParent(true); root.pad(28);
        heading = new Label("", context.skin); heading.setFontScale(1.6f); heading.setAlignment(Align.center);
        details = new Label("", context.skin); details.setWrap(true); details.setAlignment(Align.left, Align.top);
        root.add(heading).colspan(3).width(920).height(70).padBottom(12).row();
        root.add(details).colspan(3).width(920).height(440).top().left().pad(18).row();
        TextButton previous = new TextButton(context.i18n.text("guide.previous"), context.skin);
        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        TextButton next = new TextButton(context.i18n.text("guide.next"), context.skin);
        root.add(previous).width(260).height(58).pad(8);
        root.add(back).width(260).height(58).pad(8);
        root.add(next).width(260).height(58).pad(8);
        previous.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { index = Math.floorMod(index - 1, totalEntries()); refresh(); }});
        next.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { index = (index + 1) % totalEntries(); refresh(); }});
        back.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { screens.show(new ModeSelectScreen(context, screens)); }});
        stage.addActor(root);
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
                Math.round(ultimate.summonHp), Math.round(ultimate.summonAttackDamage));
            case "moving_car" -> context.i18n.text("guide.car", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                decimal(ultimate.effectSeconds), decimal(ultimate.stunSeconds));
            case "spinning_one" -> context.i18n.text("guide.spinning_one", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE),
                decimal(ultimate.lineLength), Math.round(ultimate.healAmount));
            case "skill_barrage" -> context.i18n.text("guide.barrage",ultimate.barrageCount,decimal(ultimate.barrageInterval));
            default -> context.i18n.text("guide.area", Math.round(ultimate.baseDamage * ULTIMATE_DAMAGE_SCALE), decimal(ultimate.radius * 5f));
        };
        String passiveStats = context.i18n.text("guide.passive_effect." + character.id);
        String skillPower="bind_follow_up".equals(skill.type)
            ?context.i18n.text("guide.bind_follow_up",decimal(skill.cooldownSeconds),Math.round(skill.damages[0]*SKILL_DAMAGE_SCALE),decimal(skill.bindDurations[0]),Math.round(skill.followUpDamage*SKILL_DAMAGE_SCALE),Math.round(points))
            :context.i18n.text("guide.skill_stats", decimal(skill.cooldownSeconds), Math.round(skill.baseDamage * SKILL_DAMAGE_SCALE), skill.glyphs.length,
                decimal(Math.max(3.2f, skill.projectileRadius * 5f) * 2f), Math.round(points), stun);
        if(skill.healthCostRatio>0)skillPower+=" / "+context.i18n.text("guide.health_cost",Math.round(skill.healthCostRatio*100));
        details.setText(
            context.i18n.text("guide.character." + character.id) + "\n\n" +
            context.i18n.text("guide.stats", Math.round(character.maxHp), decimal(character.moveSpeed)) + "\n\n" +
            context.i18n.text("guide.skill", context.i18n.text("ability." + character.id + ".skill.name")) + "\n" +
            skillPower + "\n\n" +
            context.i18n.text("guide.ultimate", context.i18n.text(ultimate.nameKey)) + "\n" + ultimatePower + "  " +
            context.i18n.text("guide.cost", Math.round(ultimate.pointCost)) + "\n\n" +
            context.i18n.text("guide.passive", context.i18n.text("ability." + character.id + ".passive.name")) + "\n" +
            passiveStats
        );
    }
    private void refreshSupport(SupportCharacterDefinition support){
        heading.setText(support.displayGlyph+"  "+context.i18n.text(support.nameKey)+"  "+(index+1)+"/"+totalEntries());
        details.setText(context.i18n.text("guide.support_role")+"\n\n"+
            context.i18n.text("guide.support_stats",decimal(support.moveSpeed),decimal(support.followDistance),decimal(support.attackInterval),Math.round(support.baseDamage),decimal(support.attackRange))+"\n\n"+
            context.i18n.text("guide.support_skill",context.i18n.text(support.skill.nameKey),decimal(support.skill.cooldownSeconds),decimal(support.skill.durationSeconds),Math.round(support.skill.magnitude*100))+"\n"+
            context.i18n.text("select.support_skill_detail."+support.id)+"\n\n"+
            context.i18n.text("guide.support_ultimate",context.i18n.text(support.ultimate.nameKey),Math.round(support.ultimate.pointCost))+"\n"+
            context.i18n.text("select.support_ultimate_detail."+support.id)+"\n\n"+
            context.i18n.text("guide.field_passives_title")+"\n"+fieldDetails(support)+"\n\n"+
            context.i18n.text("select.support_style."+support.id));
    }
    private String fieldDetails(SupportCharacterDefinition support){return String.join("\n",java.util.Arrays.stream(support.fieldPassiveIds).map(id->context.i18n.text("field."+id)+": "+context.i18n.text("field.description."+id)).toList());}
    private int totalEntries(){return characters.size()+supports.size();}

    private static String decimal(float value) { return String.format("%.1f", value); }
}
