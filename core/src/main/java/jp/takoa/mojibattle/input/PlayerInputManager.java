package jp.takoa.mojibattle.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import jp.takoa.mojibattle.config.ControlConfig;
import jp.takoa.mojibattle.domain.PlayerId;

import java.util.EnumMap;

/**
 * キーボードとJoy-Conのコマンド生成を、全体共通の入力方式に従って切り替える。
 */
public final class PlayerInputManager extends InputAdapter {
    private final InputBindingManager keyboard;
    private final JoyConInputManager joyCons;
    private final InputDeviceMode mode;
    private final boolean diagnostics = Boolean.getBoolean("mojibattle.controllerDiagnostics");
    private final EnumMap<PlayerId, String> diagnosticCommands = new EnumMap<>(PlayerId.class);

    public PlayerInputManager(ControlConfig controls, JoyConInputManager joyCons, InputDeviceMode mode) {
        keyboard = new InputBindingManager(controls);
        this.joyCons = joyCons;
        this.mode = mode;
        if (mode == InputDeviceMode.CONTROLLER) {
            joyCons.resetInputState();
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return keyboard.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return keyboard.keyUp(keycode);
    }

    public void refreshControllers() {
        if (mode == InputDeviceMode.CONTROLLER) {
            joyCons.refresh();
        }
    }

    public PlayerCommand consume(PlayerId player) {
        PlayerCommand command = mode == InputDeviceMode.CONTROLLER ? joyCons.consume(player) : keyboard.consume(player);
        if (diagnostics && mode == InputDeviceMode.CONTROLLER && Gdx.app != null) {
            String value = command.toString();
            if (!value.equals(diagnosticCommands.put(player, value))) {
                Gdx.app.log("PlayerInput", "[PlayerInputManager] " + player.label() + " " + value);
            }
        }
        return command;
    }

    /**
     * ポーズは入力方式に関係なく既存キーボード割当だけから取得する。
     */
    public boolean consumePause() {
        return keyboard.consumePause();
    }

    public void clear() {
        keyboard.clear();
        if (mode == InputDeviceMode.CONTROLLER) {
            joyCons.resetInputState();
        }
    }
}
