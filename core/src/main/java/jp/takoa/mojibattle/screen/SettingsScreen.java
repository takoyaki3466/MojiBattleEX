package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.input.InputDeviceMode;
import jp.takoa.mojibattle.ui.UiTheme;
import jp.takoa.mojibattle.ui.UiThemeManager;

/**
 * 解像度、全画面、音量、UI倍率、カメラ追従値をPreferencesへ保存する設定画面。
 */
public final class SettingsScreen extends StageScreen {
    private static final int[][] RESOLUTIONS = {{1280, 720}, {1600, 900}, {1920, 1080}};

    public SettingsScreen(GameContext context, ScreenManager screens) {
        super(context, screens);
        Preferences preferences = Gdx.app.getPreferences(UiThemeManager.PREFERENCES_NAME);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        Table table = new Table(context.skin);
        table.setBackground("panel");
        table.pad(26);
        table.defaults().growX().height(42).pad(2);

        Label title = new Label(context.i18n.text("menu.settings"), context.skin, "title");
        title.setFontScale(.54f);
        title.setAlignment(Align.center);
        table.add(title).height(62).row();

        int[] resolutionIndex = {1};

        TextButton resolutionButton = new TextButton(context.i18n.text("settings.resolution", context.config.graphics.windowWidth + "x" + context.config.graphics.windowHeight), context.skin);

        table.add(resolutionButton).row();

        resolutionButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                resolutionIndex[0] = (resolutionIndex[0] + 1) % RESOLUTIONS.length;

                int width = RESOLUTIONS[resolutionIndex[0]][0];
                int height = RESOLUTIONS[resolutionIndex[0]][1];

                context.config.graphics.windowWidth = width;
                context.config.graphics.windowHeight = height;

                preferences.putInteger("windowWidth", width).putInteger("windowHeight", height).flush();

                if (!Gdx.graphics.isFullscreen()) {
                    Gdx.graphics.setWindowedMode(width, height);
                }

                resolutionButton.setText(context.i18n.text("settings.resolution", width + "x" + height));
            }
        });

        TextButton fullscreen = new TextButton(context.i18n.text("settings.fullscreen", preferences.getBoolean("fullscreen", context.config.graphics.fullscreen)), context.skin);
        table.add(fullscreen).row();

        fullscreen.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                boolean enabled = !preferences.getBoolean("fullscreen", context.config.graphics.fullscreen);

                preferences.putBoolean("fullscreen", enabled).flush();

                if (enabled) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    Gdx.graphics.setWindowedMode(context.config.graphics.windowWidth, context.config.graphics.windowHeight);
                }

                fullscreen.setText(context.i18n.text("settings.fullscreen", enabled));
            }
        });

        TextButton volume = new TextButton(context.i18n.text("settings.volume", Math.round(preferences.getFloat("volume", context.config.game.masterVolume) * 100)), context.skin);
        table.add(volume).row();

        volume.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                float value = preferences.getFloat("volume", context.config.game.masterVolume) + .1f;

                if (value > 1.001f) {
                    value = 0;
                }

                preferences.putFloat("volume", value).flush();
                volume.setText(context.i18n.text("settings.volume", Math.round(value * 100)));
            }
        });

        TextButton uiScale = new TextButton(context.i18n.text("settings.ui_scale", preferences.getFloat("uiScale", context.config.game.uiScale)), context.skin);
        table.add(uiScale).row();

        uiScale.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                float value = preferences.getFloat("uiScale", context.config.game.uiScale) + .25f;

                if (value > 2) {
                    value = .75f;
                }

                preferences.putFloat("uiScale", value).flush();
                uiScale.setText(context.i18n.text("settings.ui_scale", value));
            }
        });

        TextButton camera = new TextButton(context.i18n.text("settings.camera_follow", preferences.getFloat("cameraFollow", context.config.graphics.cameraFollowSpeed)), context.skin);
        table.add(camera).row();

        camera.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                float value = preferences.getFloat("cameraFollow", context.config.graphics.cameraFollowSpeed) + 1;

                if (value > 12) {
                    value = 3;
                }

                context.config.graphics.cameraFollowSpeed = value;
                preferences.putFloat("cameraFollow", value).flush();
                camera.setText(context.i18n.text("settings.camera_follow", value));
            }
        });

        boolean cutInsEnabled = preferences.getBoolean("ultimateCutIns", context.config.graphics.ultimateCutIns);
        context.config.graphics.ultimateCutIns = cutInsEnabled;
        TextButton ultimateCutIns = new TextButton(cutInLabel(cutInsEnabled), context.skin);
        table.add(ultimateCutIns).row();
        ultimateCutIns.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                boolean enabled = !preferences.getBoolean("ultimateCutIns", context.config.graphics.ultimateCutIns);
                context.config.graphics.ultimateCutIns = enabled;
                preferences.putBoolean("ultimateCutIns", enabled).flush();
                ultimateCutIns.setText(cutInLabel(enabled));
            }
        });

        UiTheme selectedTheme = UiThemeManager.selected();
        TextButton uiTheme = new TextButton(themeLabel(selectedTheme), context.skin);
        table.add(uiTheme).row();
        uiTheme.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                UiTheme next = UiThemeManager.selected().next();
                UiThemeManager.select(context.skin, next);
                screens.show(new SettingsScreen(context, screens));
            }
        });

        InputDeviceMode selectedInput = InputDeviceMode.load(preferences);
        TextButton inputMode = new TextButton(inputModeLabel(selectedInput), context.skin);
        table.add(inputMode).row();

        inputMode.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                InputDeviceMode next = InputDeviceMode.load(preferences).next();
                next.save(preferences);
                inputMode.setText(inputModeLabel(next));
            }
        });

        TextButton controllerSettings = new TextButton(context.i18n.text("settings.controller.open"), context.skin);
        table.add(controllerSettings).row();
        controllerSettings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new ControllerSettingsScreen(context, screens));
            }
        });

        table.add(new Label(context.i18n.text("settings.language", context.config.game.language), context.skin)).row();
        table.add(new Label(context.i18n.text("settings.keys"), context.skin)).row();

        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);

        table.add(back);
        back.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new TitleScreen(context, screens));
            }
        });

        addStandardMenu(root, table);
    }

    private String inputModeLabel(InputDeviceMode mode) {
        String valueKey = mode == InputDeviceMode.KEYBOARD ? "settings.input.keyboard" : "settings.input.controller";
        return context.i18n.text("settings.input", context.i18n.text(valueKey));
    }

    private String themeLabel(UiTheme theme) {
        return context.i18n.text("settings.ui_theme", context.i18n.text(theme.translationKey()));
    }

    private String cutInLabel(boolean enabled) {
        return context.i18n.text("settings.ultimate_cut_ins", context.i18n.text(enabled ? "settings.enabled" : "settings.disabled"));
    }
}
