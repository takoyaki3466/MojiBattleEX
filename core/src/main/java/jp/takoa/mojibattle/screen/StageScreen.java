package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jp.takoa.mojibattle.GameContext;

/**
 * Scene2Dを使用するメニュー画面に、共通背景、Viewport、描画ライフサイクルを提供する基底クラス。
 */
public abstract class StageScreen extends ScreenAdapter {
    protected static final float MENU_WIDTH_RATIO = .92f;
    protected static final float MENU_HEIGHT_RATIO = .90f;

    protected final GameContext context;
    protected final ScreenManager screens;
    protected final Stage stage = new Stage(new ScreenViewport());

    private final Image background;

    protected StageScreen(GameContext context, ScreenManager screens) {
        this.context = context;
        this.screens = screens;

        background = new Image(context.skin.newDrawable("menu-green"));

        background.setScaling(Scaling.fill);
        background.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage.addActor(background);
    }

    /**
     * 共通比率でメニューをルートTableへ配置し、Stageへ追加する。
     */
    protected void addStandardMenu(Table root, Table menu) {
        addMenu(root, menu, MENU_WIDTH_RATIO, MENU_HEIGHT_RATIO);
    }

    /**
     * 画面固有の占有率でメニューを配置する。
     *
     * @param root        画面全体を覆うルートTable
     * @param menu        配置するメニュー
     * @param widthRatio  画面幅に対する比率
     * @param heightRatio 画面高さに対する比率
     */
    protected void addMenu(Table root, Table menu, float widthRatio, float heightRatio) {
        root.setFillParent(true);
        root.pad(0);
        root.add(menu).width(Value.percentWidth(widthRatio, root)).height(Value.percentHeight(heightRatio, root));

        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(.015f, .075f, .052f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        background.setBounds(0, 0, width, height);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
