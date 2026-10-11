package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jp.takoa.mojibattle.GameContext;

public abstract class StageScreen extends ScreenAdapter {
    protected final GameContext context;
    protected final ScreenManager screens;
    protected final Stage stage = new Stage(new ScreenViewport());
    private final Image background;
    protected StageScreen(GameContext context, ScreenManager screens) {
        this.context = context; this.screens = screens;
        background=new Image(context.skin.newDrawable("menu-green")); background.setScaling(Scaling.fill);
        background.setBounds(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        stage.addActor(background);
    }
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) {
        Gdx.gl.glClearColor(.015f,.075f,.052f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1f / 15f)); stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); background.setBounds(0,0,width,height); }
    @Override public void dispose() { stage.dispose(); }
}
