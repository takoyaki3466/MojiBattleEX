package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;

/**
 * UI 3専用のメニュー配置。斜めボタンを交互にずらし、警告帯とネオンレールを各段へ重ねる。
 */
public final class ChaosMenuLayout {
    private final Skin skin;
    private final Table surface;
    private int actionIndex;

    public ChaosMenuLayout(Skin skin) {
        this.skin = skin;
        surface = new Table(skin);
        surface.setBackground("panel");
        surface.pad(12, 20, 14, 20);
        surface.defaults().growX();
        surface.add(rail("gauge-ultimate")).height(8).padBottom(4).row();
    }

    public Table table() {
        return surface;
    }

    public ChaosMenuLayout header(Label title, Label subtitle) {
        title.setAlignment(Align.center);
        surface.add(title).height(116).pad(0, 38, 0, 38).row();

        if (subtitle != null) {
            subtitle.setAlignment(Align.center);
            surface.add(subtitle).height(45).pad(0, 100, 2, 100).row();
        }

        surface.add(rail("gauge-skill")).height(7).pad(0, 56, 7, 56).row();
        return this;
    }

    /**
     * 大型ボタンを左右交互にずらし、参考画像の不均衡な勢いを作る。
     */
    public ChaosMenuLayout action(Actor button, float height) {
        Table row = new Table();
        boolean right = (actionIndex++ & 1) == 1;
        float leading = right ? 120 : 34;
        float trailing = right ? 34 : 120;

        row.add(rail(actionIndex % 3 == 0 ? "gauge-ultimate" : "gauge-support-skill")).width(12).height(height - 18).padRight(7);
        row.add(button).width(Value.percentWidth(.78f, row)).height(height);
        row.add(rail(actionIndex % 2 == 0 ? "gauge-support-ultimate" : "gauge-skill")).width(12).height(height - 18).padLeft(7);
        surface.add(row).growX().height(height + 8).padLeft(leading).padRight(trailing).row();
        return this;
    }

    /**
     * 対戦人数画面用の左右対向ボタン列。
     */
    public ChaosMenuLayout pair(Actor left, Actor right, float height) {
        Table row = new Table();
        row.add(left).growX().height(height).padRight(4);
        row.add(rail("gauge-ultimate")).width(9).height(height - 12).pad(0, 3, 0, 3);
        row.add(right).growX().height(height).padLeft(4);
        surface.add(row).growX().height(height + 7).pad(0, 18, 0, 18).row();
        return this;
    }

    public ChaosMenuLayout notice(Actor notice) {
        surface.add(notice).height(38).pad(2, 100, 2, 100).row();
        return this;
    }

    public ChaosMenuLayout finish() {
        surface.add(rail("gauge-support-ultimate")).height(8).padTop(7).row();
        return this;
    }

    /**
     * キャラクター選択専用の、左右分割＋下部プレイヤー帯レイアウト。
     */
    public static Table selection(Skin skin, Table roster, Table information, Table playerPreviews) {
        Table upper = new Table();
        upper.add(rail(skin, "gauge-skill")).width(8).growY().padRight(3);
        upper.add(roster).grow().minWidth(0).minHeight(0).top().padRight(5);
        upper.add(information).width(Value.percentWidth(.31f, upper)).growY().minWidth(330).minHeight(0).top().padLeft(5);
        upper.add(rail(skin, "gauge-support-ultimate")).width(8).growY().padLeft(3);

        Table shell = new Table(skin);
        shell.setBackground("panel");
        shell.pad(5);
        shell.add(rail(skin, "gauge-ultimate")).growX().height(7).row();
        shell.add(upper).grow().minWidth(0).minHeight(0).top().padTop(3).row();
        shell.add(rail(skin, "gauge-skill")).growX().height(7).padTop(3).row();
        shell.add(playerPreviews).growX().height(116).padTop(3);
        return shell;
    }

    private Image rail(String drawable) {
        return rail(skin, drawable);
    }

    private static Image rail(Skin skin, String drawable) {
        return new Image(skin.newDrawable(drawable));
    }
}
