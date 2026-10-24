package jp.takoa.mojibattle.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.input.InputDeviceMode;
import jp.takoa.mojibattle.input.JoyConLiveState;
import jp.takoa.mojibattle.input.JoyConSide;
import jp.takoa.mojibattle.ui.UiThemeManager;

import java.util.EnumMap;
import java.util.Locale;

/**
 * Joy-Conの割り当て、接続状態、ゲーム内アクション入力を確認する専用画面。
 */
public final class ControllerSettingsScreen extends StageScreen {
    private static final float ACTIVE_R = .35f;
    private static final float ACTIVE_G = 1f;
    private static final float ACTIVE_B = .55f;
    private static final float INACTIVE_R = .48f;
    private static final float INACTIVE_G = .58f;
    private static final float INACTIVE_B = .52f;

    private final EnumMap<PlayerId, PlayerPanel> playerPanels = new EnumMap<>(PlayerId.class);
    private final Label mode;
    private final Label unassigned;

    public ControllerSettingsScreen(GameContext context, ScreenManager screens) {
        super(context, screens);

        Table root = new Table();
        Table panel = new Table(context.skin);
        panel.setBackground("panel");
        panel.pad(22);

        Label title = new Label(context.i18n.text("controller.title"), context.skin, "title");
        title.setFontScale(.58f);
        title.setAlignment(Align.center);
        panel.add(title).growX().height(56).row();

        Label help = new Label(context.i18n.text("controller.help"), context.skin);
        help.setAlignment(Align.center);
        help.setWrap(true);
        help.setColor(.62f, 1f, .76f, 1f);
        panel.add(help).growX().height(44).padBottom(8).row();

        Table cards = new Table();
        cards.defaults().grow().pad(7);
        for (PlayerId player : PlayerId.values()) {
            PlayerPanel playerPanel = createPlayerPanel(player);
            playerPanels.put(player, playerPanel);
            cards.add(playerPanel.table()).minWidth(390).minHeight(190);
            if (player == PlayerId.TWO) {
                cards.row();
            }
        }
        panel.add(cards).grow().row();

        Table footer = new Table();
        mode = new Label("", context.skin);
        mode.setAlignment(Align.left);
        unassigned = new Label("", context.skin);
        unassigned.setAlignment(Align.center);
        TextButton back = new TextButton(context.i18n.text("menu.back"), context.skin);
        footer.add(mode).growX().height(44);
        footer.add(unassigned).growX().height(44).padLeft(12).padRight(12);
        footer.add(back).width(190).height(44);
        panel.add(footer).growX().height(48).padTop(8);

        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                screens.show(new SettingsScreen(context, screens));
            }
        });

        addMenu(root, panel, .96f, .96f);
        updateLiveState();
    }

    private PlayerPanel createPlayerPanel(PlayerId player) {
        Table card = new Table(context.skin);
        card.setBackground("panel");
        card.pad(16);
        card.defaults().growX().pad(3);

        Label playerName = new Label(player.label(), context.skin, "title");
        playerName.setFontScale(.42f);
        playerName.setAlignment(Align.left);
        Label connection = new Label("", context.skin);
        connection.setAlignment(Align.right);
        Table heading = new Table();
        heading.add(playerName).growX().height(36);
        heading.add(connection).width(150).height(36);
        card.add(heading).height(38).row();

        Label device = new Label("", context.skin);
        device.setAlignment(Align.left);
        device.setWrap(true);
        card.add(device).height(36).row();

        Label stick = new Label("", context.skin);
        stick.setAlignment(Align.left);
        card.add(stick).height(30).row();

        Label mainSkill = actionLabel("controller.action.main_skill");
        Label mainUltimate = actionLabel("controller.action.main_ultimate");
        Label supportSkill = actionLabel("controller.action.support_skill");
        Label supportUltimate = actionLabel("controller.action.support_ultimate");
        Table actions = new Table();
        actions.defaults().growX().height(27).pad(2);
        actions.add(mainSkill);
        actions.add(mainUltimate).row();
        actions.add(supportSkill);
        actions.add(supportUltimate);
        card.add(actions).height(62).row();

        TextButton assign = new TextButton(context.i18n.text("controller.assign", player.label()), context.skin);
        card.add(assign).height(38).padTop(7);
        assign.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                context.joyCons.cycleAssignment(player);
                updateLiveState();
            }
        });

        return new PlayerPanel(card, connection, device, stick, mainSkill, mainUltimate, supportSkill, supportUltimate);
    }

    private Label actionLabel(String key) {
        Label label = new Label(context.i18n.text("controller.action.inactive", context.i18n.text(key)), context.skin);
        label.setAlignment(Align.center);
        label.setColor(INACTIVE_R, INACTIVE_G, INACTIVE_B, 1f);
        return label;
    }

    @Override
    public void render(float delta) {
        updateLiveState();
        super.render(delta);
    }

    private void updateLiveState() {
        Preferences preferences = Gdx.app.getPreferences(UiThemeManager.PREFERENCES_NAME);
        InputDeviceMode inputMode = InputDeviceMode.load(preferences);
        String inputName = context.i18n.text(inputMode == InputDeviceMode.CONTROLLER ? "settings.input.controller" : "settings.input.keyboard");
        mode.setText(context.i18n.text("controller.input_mode", inputName));

        for (JoyConLiveState state : context.joyCons.liveStates()) {
            updatePlayerPanel(playerPanels.get(state.player()), state);
        }
        int count = context.joyCons.unassignedCount();
        unassigned.setText(count == 0 ? "" : context.i18n.text("settings.controller.unassigned", count));
    }

    private void updatePlayerPanel(PlayerPanel panel, JoyConLiveState state) {
        panel.connection().setText(context.i18n.text(state.connected() ? "settings.controller.connected" : "settings.controller.disconnected"));
        if (state.connected()) {
            panel.connection().setColor(.35f, 1f, .55f, 1f);
        } else {
            panel.connection().setColor(1f, .46f, .40f, 1f);
        }

        if (state.side() == null) {
            panel.device().setText(context.i18n.text("controller.no_device"));
        } else {
            String side = context.i18n.text(state.side() == JoyConSide.LEFT ? "settings.controller.left" : "settings.controller.right");
            panel.device().setText(context.i18n.text("controller.device", side, state.name()));
        }
        panel.stick().setText(stickText(state.movement()));
        setAction(panel.mainSkill(), "controller.action.main_skill", state.mainSkill());
        setAction(panel.mainUltimate(), "controller.action.main_ultimate", state.mainUltimate());
        setAction(panel.supportSkill(), "controller.action.support_skill", state.supportSkill());
        setAction(panel.supportUltimate(), "controller.action.support_ultimate", state.supportUltimate());
    }

    private String stickText(Vector2 movement) {
        if (movement.isZero(.01f)) {
            return context.i18n.text("controller.stick", context.i18n.text("controller.stick.neutral"), "0.00", "0.00");
        }
        String horizontal = movement.x > .25f ? context.i18n.text("controller.direction.right") : movement.x < -.25f ? context.i18n.text("controller.direction.left") : "";
        String vertical = movement.y > .25f ? context.i18n.text("controller.direction.up") : movement.y < -.25f ? context.i18n.text("controller.direction.down") : "";
        String direction = vertical + horizontal;
        return context.i18n.text("controller.stick", direction, String.format(Locale.ROOT, "%.2f", movement.x), String.format(Locale.ROOT, "%.2f", movement.y));
    }

    private void setAction(Label label, String key, boolean active) {
        label.setText(context.i18n.text(active ? "controller.action.active" : "controller.action.inactive", context.i18n.text(key)));
        label.setColor(active ? ACTIVE_R : INACTIVE_R, active ? ACTIVE_G : INACTIVE_G, active ? ACTIVE_B : INACTIVE_B, 1f);
    }

    private record PlayerPanel(Table table, Label connection, Label device, Label stick, Label mainSkill,
                               Label mainUltimate, Label supportSkill, Label supportUltimate) {
    }
}
