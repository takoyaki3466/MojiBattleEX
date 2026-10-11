package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.*;

import java.util.EnumMap;
import java.util.List;

public final class BattleHud implements Disposable {
    private final GameContext context;
    private final Stage stage=new Stage(new ScreenViewport());
    private final EnumMap<PlayerId,PlayerHud> playerHuds=new EnumMap<>(PlayerId.class);
    private final Label timer,center,training;
    private final Image flash;
    private float rainbowPhase;

    public BattleHud(GameContext context,BattleController battle){
        this.context=context;
        flash=new Image(context.skin.newDrawable("bar-hp"));flash.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);stage.addActor(flash);
        Table grid=new Table();grid.setFillParent(true);grid.pad(8);
        List<PlayerId> ids=battle.playerIds();float scale=ids.size()>2?.58f:.72f;
        for(int index=0;index<ids.size();index++){
            PlayerHud hud=new PlayerHud(ids.get(index),scale);playerHuds.put(ids.get(index),hud);
            grid.add(hud.panel).expand().fill().pad(5);if(index%2==1)grid.row();
        }
        if(ids.size()==3)grid.add(new Table()).expand();stage.addActor(grid);
        timer=new Label("",context.skin);timer.setFontScale(.9f);Table clock=new Table();clock.setFillParent(true);clock.top();clock.add(timer).pad(7);stage.addActor(clock);
        center=new Label("",context.skin,"effect-text");Table centerTable=new Table();centerTable.setFillParent(true);centerTable.add(center);stage.addActor(centerTable);
        training=new Label("",context.skin);Table bottom=new Table();bottom.setFillParent(true);bottom.bottom();bottom.add(training).pad(8);stage.addActor(bottom);
    }

    public void update(BattleController battle){
        rainbowPhase=(rainbowPhase+1.8f)%360f;flash.setColor(.35f,.75f,1,battle.visualPulse()*.14f);
        for(var entry:playerHuds.entrySet())entry.getValue().update(battle,battle.player(entry.getKey()));
        timer.setText(context.i18n.text("battle.time_remaining",formatTime(battle.timer().remaining())));
        if(battle.announcement()!=null){
            float remaining=battle.announcement().duration(),elapsed=2.4f-remaining,pulse=Math.abs(com.badlogic.gdx.math.MathUtils.sin(elapsed*10));
            center.setText(context.i18n.text("battle.ultimate_announcement",battle.announcement().player().label(),context.i18n.text(battle.announcement().i18nKey())));
            center.setFontScale(1.1f+pulse*.55f);center.setColor(1,.32f+pulse*.68f,.12f+(1-pulse)*.88f,Math.min(1,Math.min(elapsed*5,remaining*3)));
            flash.setColor(1,.08f+.7f*pulse,.12f,.12f+pulse*.16f);
        }else{
            center.setFontScale(1);center.setColor(Color.WHITE);center.setText(battle.criticalPulse()>0?context.i18n.text("battle.critical"):battle.finalMinuteStarted()?context.i18n.text("battle.final_minute"):"");
            if(battle.criticalPulse()>0){center.setFontScale(1.45f);center.setColor(Color.GOLD);}
        }
        if(battle.mode()==BattleMode.TRAINING){TrainingStatistics s=battle.trainingStatistics();training.setText(context.i18n.text("training.hits",s.hits())+"  "+context.i18n.text("training.dps",String.format("%.1f",s.dps()))+"  "+context.i18n.text("training.last_damage",String.format("%.1f",s.lastDamage()))+"  "+context.i18n.text("training.controls"));}else training.setText("");
    }

    private final class PlayerHud{
        final PlayerId id;
        final Table panel=new Table(),mainPanel=new Table(),supportPanel=new Table();
        final ProgressBar hp=bar(),skill=bar(),ultimate=bar(),supportSkill=bar(),supportUltimate=bar();
        final Label title=label(),hpText=label(),skillReady=label(),ultimateReady=label(),supportTitle=label(),supportSkillState=label(),supportUltimateReady=label(),details=label();

        PlayerHud(PlayerId id,float scale){
            this.id=id;mainPanel.setBackground(context.skin.getDrawable("panel"));mainPanel.pad(9);
            for(Label value:List.of(title,hpText,skillReady,ultimateReady,supportTitle,supportSkillState,supportUltimateReady,details))value.setFontScale(scale);
            title.setColor(Color.WHITE);mainPanel.add(title).growX().left().row();
            Table hpRow=new Table();hpText.setFontScale(scale*.95f);hpRow.add(hpText).width(130).left();hpRow.add(hp).growX().height(34);mainPanel.add(hpRow).growX().padTop(4).row();
            Table mainMeters=new Table();mainMeters.add(meter("battle.skill_gauge",skill,skillReady,scale)).expandX().fillX().padRight(5);mainMeters.add(meter("battle.ultimate_gauge",ultimate,ultimateReady,scale)).expandX().fillX().padLeft(5);mainPanel.add(mainMeters).growX().padTop(5).row();
            details.setWrap(true);mainPanel.add(details).growX().left().padTop(3).row();
            supportPanel.setBackground(context.skin.getDrawable("button-down"));supportPanel.pad(6);supportPanel.add(supportTitle).colspan(2).left().row();
            supportPanel.add(meter("battle.support_skill_gauge",supportSkill,supportSkillState,scale*.9f)).width(235).padRight(4);
            supportPanel.add(meter("battle.support_ultimate_gauge",supportUltimate,supportUltimateReady,scale*.9f)).width(235).padLeft(4);
            panel.add(mainPanel).growX().top().row();panel.add().expandY().row();panel.add(supportPanel).width(480).right().bottom();
        }

        private Table meter(String captionKey,ProgressBar gauge,Label state,float scale){
            Table meter=new Table();Label caption=new Label(context.i18n.text(captionKey),context.skin);caption.setFontScale(scale*.85f);meter.add(caption).left();meter.add(state).expandX().right().row();meter.add(gauge).colspan(2).growX().height(22).padTop(2);return meter;
        }

        void update(BattleController battle,CharacterRuntime player){
            SupportCharacterRuntime support=battle.support(id);
            hp.setValue(player.hpRatio());hpText.setText(context.i18n.text("battle.hp_fraction",Math.round(player.hp()),Math.round(player.definition.maxHp)));
            skill.setValue(player.skillChargeRatio());ultimate.setValue(player.ultimatePoints()/100f);supportSkill.setValue(support.skillChargeRatio());supportUltimate.setValue(support.ultimateChargeRatio());
            title.setText(id.label()+"  "+context.i18n.text(player.definition.nameKey)+(player.awakened()?"  "+context.i18n.text("battle.awakened"):""));
            supportTitle.setText(context.i18n.text("battle.support",context.i18n.text(support.definition.nameKey)));
            setCooldownState(skillReady,player.skillReady(),player.skillCooldown(),"battle.skill_available",false);
            setUltimateState(ultimateReady,player.ultimatePoints(),"battle.ultimate_available");
            setCooldownState(supportSkillState,false,support.skillCooldown(),"battle.support_skill_available",true);
            setUltimateState(supportUltimateReady,support.ultimatePoints(),"battle.support_ultimate_available");
            String fields=String.join(" / ",battle.fieldEffectIds().stream().map(key->context.i18n.text("field."+key)).toList());details.setText(statusBadges(player)+"  "+context.i18n.text("battle.field_effects",fields));
        }
        void setReady(Label label,boolean ready,String key){label.setText(ready?context.i18n.text(key):"");if(ready)label.setColor(new Color().fromHsv(rainbowPhase+id.ordinal()*70,1,1));}
        void setCooldownState(Label label,boolean ready,float remaining,String readyKey,boolean automatic){if(ready){setReady(label,true,readyKey);return;}label.setColor(Color.WHITE);label.setText(context.i18n.text(automatic?"battle.auto_cooldown":"battle.cooldown_seconds",String.format("%.1f",remaining)));}
        void setUltimateState(Label label,float points,String readyKey){if(points>=100){setReady(label,true,readyKey);return;}label.setColor(Color.WHITE);label.setText(context.i18n.text("battle.percent",Math.round(points)));}
    }

    private ProgressBar bar(){return new ProgressBar(0,1,.001f,false,context.skin);}
    private Label label(){return new Label("",context.skin);}
    private String statusBadges(CharacterRuntime player){if(player.statuses.active().isEmpty())return "";StringBuilder value=new StringBuilder("[");for(StatusEffect effect:player.statuses.active()){if(value.length()>1)value.append(' ');value.append(effect.type.name());}return value.append(']').toString();}
    private static String formatTime(float seconds){int total=Math.max(0,(int)Math.ceil(seconds));return "%d:%02d".formatted(total/60,total%60);}
    public void draw(BattleController battle){update(battle);stage.act();stage.draw();}
    public void resize(int width,int height){stage.getViewport().update(width,height,true);flash.setBounds(0,0,width,height);}
    public Stage stage(){return stage;}
    @Override public void dispose(){stage.dispose();}
}
