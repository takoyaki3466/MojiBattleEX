package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import jp.takoa.mojibattle.GameContext;

public final class SettingsScreen extends StageScreen {
    public SettingsScreen(GameContext context, ScreenManager screens) {
        super(context, screens); Preferences preferences = Gdx.app.getPreferences("mojibattle-user");
        Table table = new Table(); table.setFillParent(true); table.defaults().width(420).height(56).pad(7);
        table.add(new Label(context.i18n.text("menu.settings"), context.skin)).row();
        int[][] resolutions = {{1280,720},{1600,900},{1920,1080}};
        int[] resolutionIndex = {1};
        TextButton resolutionButton = new TextButton(context.i18n.text("settings.resolution", context.config.graphics.windowWidth + "x" + context.config.graphics.windowHeight), context.skin);
        table.add(resolutionButton).row();
        resolutionButton.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) {
            resolutionIndex[0] = (resolutionIndex[0] + 1) % resolutions.length;
            int width = resolutions[resolutionIndex[0]][0], height = resolutions[resolutionIndex[0]][1];
            context.config.graphics.windowWidth = width; context.config.graphics.windowHeight = height;
            preferences.putInteger("windowWidth", width).putInteger("windowHeight", height).flush();
            if (!Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(width, height);
            resolutionButton.setText(context.i18n.text("settings.resolution", width + "x" + height));
        }});
        TextButton fullscreen = new TextButton(context.i18n.text("settings.fullscreen", preferences.getBoolean("fullscreen", context.config.graphics.fullscreen)), context.skin);
        table.add(fullscreen).row();
        fullscreen.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) {
            boolean enabled = !preferences.getBoolean("fullscreen", context.config.graphics.fullscreen);
            preferences.putBoolean("fullscreen", enabled).flush();
            if (enabled) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            else Gdx.graphics.setWindowedMode(context.config.graphics.windowWidth, context.config.graphics.windowHeight);
            fullscreen.setText(context.i18n.text("settings.fullscreen", enabled));
        }});
        TextButton volume = new TextButton(context.i18n.text("settings.volume", Math.round(preferences.getFloat("volume", context.config.game.masterVolume) * 100)), context.skin);
        table.add(volume).row();
        volume.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) {
            float value = preferences.getFloat("volume", context.config.game.masterVolume) + .1f; if (value > 1.001f) value = 0;
            preferences.putFloat("volume", value).flush(); volume.setText(context.i18n.text("settings.volume", Math.round(value * 100)));
        }});
        TextButton uiScale = new TextButton(context.i18n.text("settings.ui_scale", preferences.getFloat("uiScale", context.config.game.uiScale)), context.skin);
        table.add(uiScale).row();
        uiScale.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) {
            float value = preferences.getFloat("uiScale", context.config.game.uiScale) + .25f; if (value > 2) value = .75f;
            preferences.putFloat("uiScale", value).flush(); uiScale.setText(context.i18n.text("settings.ui_scale", value));
        }});
        TextButton camera = new TextButton(context.i18n.text("settings.camera_follow", preferences.getFloat("cameraFollow", context.config.graphics.cameraFollowSpeed)), context.skin);
        table.add(camera).row();
        camera.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) {
            float value = preferences.getFloat("cameraFollow", context.config.graphics.cameraFollowSpeed) + 1; if (value > 12) value = 3;
            context.config.graphics.cameraFollowSpeed = value;
            preferences.putFloat("cameraFollow", value).flush(); camera.setText(context.i18n.text("settings.camera_follow", value));
        }});
        table.add(new Label(context.i18n.text("settings.language", context.config.game.language), context.skin)).row();
        table.add(new Label(context.i18n.text("settings.keys"), context.skin)).row();
        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin); table.add(back);
        back.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { screens.show(new TitleScreen(context, screens)); }});
        stage.addActor(table);
    }
}
