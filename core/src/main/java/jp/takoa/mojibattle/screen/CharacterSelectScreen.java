package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.*;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;

import java.util.*;
import java.util.List;

public final class CharacterSelectScreen extends StageScreen {
    private static final float SKILL_DAMAGE_SCALE=.55f,ULTIMATE_DAMAGE_SCALE=.65f;
    private final BattleMode mode;
    private final int playerCount;
    private final Label prompt,chosen,details;
    private final Image preview=new Image();
    private final Table cardGrid=new Table();
    private final TextButton confirm,cancel;
    private final Map<String,Texture> previewTextures=new HashMap<>();
    private final List<String> mainIds=new ArrayList<>(),supportIds=new ArrayList<>();
    private Table selectedCard;
    private String pendingId;
    private int phase;

    public CharacterSelectScreen(GameContext context,ScreenManager screens,BattleMode mode,int playerCount){
        super(context,screens);this.mode=mode;this.playerCount=mode==BattleMode.TRAINING?1:Math.max(2,Math.min(4,playerCount));
        Table root=new Table();root.setFillParent(true);root.pad(18);
        Table roster=new Table(context.skin);roster.setBackground("panel");roster.pad(14);
        Label heading=new Label(context.i18n.text("select.title"),context.skin,"title");heading.setFontScale(.48f);roster.add(heading).width(700).height(62).left().row();
        prompt=new Label("",context.skin);prompt.setColor(.62f,1f,.76f,1f);roster.add(prompt).left().row();
        chosen=new Label("",context.skin);chosen.setFontScale(.62f);roster.add(chosen).left().padBottom(8).row();
        roster.add(cardGrid).width(700).height(440).top().left();

        Table information=new Table(context.skin);information.setBackground("panel");information.pad(16);
        preview.setScaling(Scaling.fit);information.add(preview).size(180,180).row();
        details=new Label(context.i18n.text("select.choose_card_hint"),context.skin);details.setWrap(true);details.setAlignment(Align.left,Align.top);details.setFontScale(.68f);information.add(details).width(470).height(330).top().left().padTop(8).row();
        Table decisions=new Table();cancel=new TextButton(context.i18n.text("select.cancel"),context.skin);confirm=new TextButton(context.i18n.text("select.confirm"),context.skin);confirm.setDisabled(true);
        decisions.add(cancel).width(210).height(58).padRight(7);decisions.add(confirm).width(240).height(58).padLeft(7);information.add(decisions).padTop(8);
        cancel.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){clearCandidate();}});
        confirm.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){confirmCandidate();}});
        root.add(roster).width(730).height(680).top().padRight(10);root.add(information).width(510).height(680).top().padLeft(10);stage.addActor(root);
        updatePrompt();refreshCards();
    }

    private void refreshCards(){
        cardGrid.clearChildren();selectedCard=null;pendingId=null;confirm.setDisabled(true);preview.setDrawable(null);details.setText(context.i18n.text("select.choose_card_hint"));
        int columns=5,index=0;
        if(selectingMain())for(CharacterDefinition character:context.characters.all()){addCard(character.id,character.glyphTexture,character.uiLabel==null?character.displayGlyph:character.uiLabel,context.i18n.text(character.nameKey),()->showMain(character));if(++index%columns==0)cardGrid.row();}
        else for(SupportCharacterDefinition support:context.supports.all()){addCard(support.id,support.glyphTexture,support.displayGlyph,context.i18n.text(support.nameKey),()->showSupport(support));if(++index%columns==0)cardGrid.row();}
    }

    private void addCard(String id,String texturePath,String glyph,String summary,Runnable detailsAction){
        Table card=new Table(context.skin);card.setBackground("button");card.pad(6);card.setTouchable(Touchable.enabled);
        Image image=new Image(drawable(texturePath));image.setScaling(Scaling.fit);Label glyphLabel=new Label(glyph,context.skin);glyphLabel.setFontScale(.7f);glyphLabel.setAlignment(Align.center);
        Label summaryLabel=new Label(summary,context.skin);summaryLabel.setWrap(true);summaryLabel.setAlignment(Align.center);summaryLabel.setFontScale(.47f);
        card.add(image).size(78,78).row();card.add(glyphLabel).width(120).height(22).row();card.add(summaryLabel).width(120).height(42).top();
        card.addListener(new InputListener(){@Override public boolean touchDown(InputEvent event,float x,float y,int pointer,int button){if(selectedCard!=null)selectedCard.setBackground("button");selectedCard=card;card.setBackground("button-down");pendingId=id;confirm.setDisabled(false);detailsAction.run();return true;}});
        cardGrid.add(card).width(132).height(158).pad(4).top();
    }

    private TextureRegionDrawable drawable(String path){Texture texture=previewTextures.computeIfAbsent(path,key->{Texture created=new Texture(com.badlogic.gdx.Gdx.files.internal(key),true);created.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);return created;});return new TextureRegionDrawable(new TextureRegion(texture));}
    private void showTexture(String path){preview.setDrawable(drawable(path));}

    private void showMain(CharacterDefinition character){
        showTexture(character.glyphTexture);AbilityConfigs.ProjectileSkill skill=context.config.skill(character.skillId);AbilityConfigs.AreaUltimate ultimate=context.config.ultimate(character.ultimateId);
        details.setText("- "+context.i18n.text("guide.skill",context.i18n.text("ability."+character.id+".skill.name"))+"\n  "+skillDetail(skill)+"\n- "+context.i18n.text("guide.ultimate",context.i18n.text(ultimate.nameKey))+"\n  "+ultimateDetail(ultimate)+" / "+context.i18n.text("guide.cost",Math.round(ultimate.pointCost))+"\n- "+context.i18n.text("guide.passive",context.i18n.text("ability."+character.id+".passive.name"))+"\n  "+context.i18n.text("guide.passive_effect."+character.id)+"\n- "+context.i18n.text("select.style",context.i18n.text("guide.character."+character.id)));
    }

    private void showSupport(SupportCharacterDefinition support){
        showTexture(support.glyphTexture);
        details.setText("- "+context.i18n.text("guide.support_skill",context.i18n.text(support.skill.nameKey),decimal(support.skill.cooldownSeconds),decimal(support.skill.durationSeconds),Math.round(support.skill.magnitude*100))+"\n  "+context.i18n.text("select.support_skill_detail."+support.id)+"\n- "+context.i18n.text("guide.support_ultimate",context.i18n.text(support.ultimate.nameKey),Math.round(support.ultimate.pointCost))+"\n  "+context.i18n.text("select.support_ultimate_detail."+support.id)+"\n- "+context.i18n.text("guide.field_passives_title")+"\n  "+fieldDetails(support)+"\n- "+context.i18n.text("select.style",context.i18n.text("select.support_style."+support.id)));
    }

    private String fieldDetails(SupportCharacterDefinition support){return String.join("\n  ",Arrays.stream(support.fieldPassiveIds).map(id->context.i18n.text("field."+id)+": "+context.i18n.text("field.description."+id)).toList());}
    private String skillDetail(AbilityConfigs.ProjectileSkill skill){int count="bind_follow_up".equals(skill.type)?1:skill.glyphs.length;float points=skill.ultimatePointsPerHit*(skill.pointsOnHit?1:count);String health=skill.healthCostRatio>0?" / "+context.i18n.text("guide.health_cost",Math.round(skill.healthCostRatio*100)):"";if("bind_follow_up".equals(skill.type))return context.i18n.text("guide.bind_follow_up",decimal(skill.cooldownSeconds),Math.round(skill.damages[0]*SKILL_DAMAGE_SCALE),decimal(skill.bindDurations[0]),Math.round(skill.followUpDamage*SKILL_DAMAGE_SCALE),Math.round(points))+health;String stun=skill.stunSeconds>0?context.i18n.text("guide.stun",decimal(skill.stunSeconds)):context.i18n.text("guide.no_stun");return context.i18n.text("guide.skill_stats",decimal(skill.cooldownSeconds),Math.round(skill.baseDamage*SKILL_DAMAGE_SCALE),count,decimal(Math.max(3.2f,skill.projectileRadius*5f)*2f),Math.round(points),stun)+health;}
    private String ultimateDetail(AbilityConfigs.AreaUltimate ultimate){return switch(ultimate.type){case "self_heal"->context.i18n.text("guide.heal",Math.round(ultimate.healAmount));case "line"->context.i18n.text("guide.line",Math.round(ultimate.baseDamage*ULTIMATE_DAMAGE_SCALE),decimal(ultimate.lineLength),decimal(ultimate.lineWidth*5.2f),Math.max(1,ultimate.directions));case "meteor_shower"->context.i18n.text("guide.meteor",ultimate.meteorCount,Math.round(ultimate.baseDamage*ULTIMATE_DAMAGE_SCALE),decimal(ultimate.radius*5f));case "summon_family"->context.i18n.text("guide.family_ultimate",ultimate.summonCount,Math.round(ultimate.summonHp),Math.round(ultimate.summonAttackDamage));case "moving_car"->context.i18n.text("guide.car",Math.round(ultimate.baseDamage*ULTIMATE_DAMAGE_SCALE),decimal(ultimate.effectSeconds),decimal(ultimate.stunSeconds));case "spinning_one"->context.i18n.text("guide.spinning_one",Math.round(ultimate.baseDamage*ULTIMATE_DAMAGE_SCALE),decimal(ultimate.lineLength),Math.round(ultimate.healAmount));case "skill_barrage"->context.i18n.text("guide.barrage",ultimate.barrageCount,decimal(ultimate.barrageInterval));default->context.i18n.text("guide.area",Math.round(ultimate.baseDamage*ULTIMATE_DAMAGE_SCALE),decimal(ultimate.radius*5f));};}

    private void confirmCandidate(){if(pendingId==null)return;if(selectingMain())mainIds.add(pendingId);else supportIds.add(pendingId);phase++;if(phase==playerCount*2){startBattle();return;}updatePrompt();refreshCards();}
    private void clearCandidate(){if(selectedCard!=null)selectedCard.setBackground("button");selectedCard=null;pendingId=null;confirm.setDisabled(true);preview.setDrawable(null);details.setText(context.i18n.text("select.choose_card_hint"));}
    private boolean selectingMain(){return phase<playerCount;}
    private void updatePrompt(){int player=selectingMain()?phase+1:phase-playerCount+1;prompt.setText(context.i18n.text(selectingMain()?"select.main_prompt":"select.support_prompt",player));chosen.setText(context.i18n.text("select.selection_progress",Math.min(phase,playerCount*2),playerCount*2));}
    private void startBattle(){if(mode==BattleMode.TRAINING){mainIds.add("kabushiki");supportIds.add("t");}screens.show(new BattleScreen(context,screens,mode,List.copyOf(mainIds),List.copyOf(supportIds)));}
    private static String decimal(float value){return String.format("%.1f",value);}
    @Override public void dispose(){super.dispose();for(Texture texture:previewTextures.values())texture.dispose();}
}
