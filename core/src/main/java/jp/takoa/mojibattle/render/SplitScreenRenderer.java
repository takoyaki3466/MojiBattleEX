package jp.takoa.mojibattle.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Disposable;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.config.GraphicsConfig;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;

import java.util.List;
import java.util.EnumMap;

public final class SplitScreenRenderer implements Disposable {
    private final ArenaRenderer arena;
    private final EnumMap<PlayerId,PlayerCameraController> cameras=new EnumMap<>(PlayerId.class);
    public SplitScreenRenderer(BattleConfig battle, GraphicsConfig graphics,List<CharacterDefinition> characters,
                               List<SupportCharacterDefinition> supports,List<AbilityConfigs.ProjectileSkill> skills,
                               List<AbilityConfigs.AreaUltimate> ultimates,String awakeningTexturePath) {
        arena = new ArenaRenderer(battle,characters,supports,skills,ultimates,awakeningTexturePath);
        for(int index=0;index<characters.size();index++)cameras.put(PlayerId.values()[index],new PlayerCameraController(graphics));
    }
    public void render(BattleController battle, float delta, int width, int height) {
        arena.updateAnimation(battle.paused() ? 0 : delta);
        Gdx.gl.glViewport(0,0,width,height);
        Gdx.gl.glClearColor(.02f,.035f,.025f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        List<PlayerId> ids=battle.playerIds();
        if(ids.size()==2){int left=width/2;renderPlayer(ids.get(0),0,0,left,height,delta,battle);renderPlayer(ids.get(1),left,0,width-left,height,delta,battle);}
        else{int halfW=width/2,halfH=height/2;for(int index=0;index<ids.size();index++){int x=index%2*halfW,y=index<2?halfH:0;renderPlayer(ids.get(index),x,y,index%2==0?halfW:width-halfW,index<2?height-halfH:halfH,delta,battle);}}
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }
    private void renderPlayer(PlayerId id,int x,int y,int width,int height,float delta,BattleController battle){PlayerCameraController controller=cameras.get(id);CharacterRuntime target=battle.targetOf(id);if(target==null)target=battle.player(id);controller.update(battle.player(id),target,delta,width,height,battle.visualPulse());renderHalf(x,y,width,height,controller,battle);}
    private void renderHalf(int x,int y,int width, int height, PlayerCameraController controller, BattleController battle) {
        Gdx.gl.glViewport(x,y,width,height); Gdx.gl.glScissor(x,y,width,height);
        Gdx.gl.glClearColor(0.55f,0.78f,0.95f,1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        arena.render(controller.camera(), battle);
    }
    @Override public void dispose() { arena.dispose(); }
}
