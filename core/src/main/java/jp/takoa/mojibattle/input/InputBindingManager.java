package jp.takoa.mojibattle.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import jp.takoa.mojibattle.config.ControlConfig;
import jp.takoa.mojibattle.domain.PlayerId;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 設定されたキー状態をプレイヤー別の1フレーム入力へ変換する入力アダプター。
 */
public final class InputBindingManager extends InputAdapter {

    private record Binding(PlayerId player, PlayerAction action) {
    }

    private final Map<Integer, Binding> bindings = new HashMap<>();
    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> pressed = new HashSet<>();
    private final EnumMap<PlayerId, EnumMap<PlayerAction, Integer>> reverse = new EnumMap<>(PlayerId.class);
    private final int pauseKey;

    private boolean pausePressed;

    public InputBindingManager(ControlConfig config) {
        bind(PlayerId.ONE, config.playerOne);
        bind(PlayerId.TWO, config.playerTwo);
        bind(PlayerId.THREE, config.playerThree);
        bind(PlayerId.FOUR, config.playerFour);

        pauseKey = key(config.pause);

        if (bindings.containsKey(pauseKey)) {
            throw new IllegalArgumentException("Pause key conflicts with player input");
        }
    }

    private void bind(PlayerId player, ControlConfig.Binding c) {
        EnumMap<PlayerAction, Integer> playerKeys = new EnumMap<>(PlayerAction.class);
        reverse.put(player, playerKeys);

        add(player, PlayerAction.MOVE_FORWARD, c.forward, playerKeys);
        add(player, PlayerAction.MOVE_BACKWARD, c.backward, playerKeys);
        add(player, PlayerAction.MOVE_LEFT, c.left, playerKeys);
        add(player, PlayerAction.MOVE_RIGHT, c.right, playerKeys);
        add(player, PlayerAction.USE_SKILL, c.skill, playerKeys);
        add(player, PlayerAction.USE_ULTIMATE, c.ultimate, playerKeys);
        add(player, PlayerAction.USE_SUPPORT_SKILL, c.supportSkill, playerKeys);
        add(player, PlayerAction.USE_SUPPORT_ULTIMATE, c.supportUltimate, playerKeys);
    }

    private void add(PlayerId player, PlayerAction action, String name, EnumMap<PlayerAction, Integer> playerKeys) {
        int code = key(name);

        if (bindings.putIfAbsent(code, new Binding(player, action)) != null) {
            throw new IllegalArgumentException("Duplicate key: " + name);
        }
        playerKeys.put(action, code);
    }

    private static int key(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Key name must not be blank");
        }

        String constantName = name.trim().toUpperCase(Locale.ROOT);

        if (constantName.equals("ANY_KEY") || constantName.equals("UNKNOWN") || constantName.equals("MAX_KEYCODE") || constantName.startsWith("META_")) {
            throw new IllegalArgumentException("Key constant cannot be bound: " + name);
        }

        try {
            int code = Input.Keys.class.getField(constantName).getInt(null);

            if (code <= Input.Keys.UNKNOWN || code > Input.Keys.MAX_KEYCODE) {
                throw new IllegalArgumentException("Key constant is outside the bindable range: " + name);
            }
            return code;

        } catch (ReflectiveOperationException | SecurityException error) {
            throw new IllegalArgumentException("Unknown key constant: " + name, error);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == pauseKey) {
            pausePressed = true;
        }

        if (bindings.containsKey(keycode)) {
            if (held.add(keycode)) {
                pressed.add(keycode);
            }
            return true;
        }
        return keycode == pauseKey;
    }

    @Override
    public boolean keyUp(int keycode) {
        pressed.remove(keycode);
        return held.remove(keycode);
    }

    /**
     * 現在押下中の移動キーと未消費の能力キーを1つのコマンドへ変換する。
     *
     * @param player 入力を取得するプレイヤー
     * @return この呼出し時点の入力コマンド
     */
    public PlayerCommand consume(PlayerId player) {
        Map<PlayerAction, Integer> keys = reverse.get(player);

        float x = axis(keys, PlayerAction.MOVE_RIGHT, PlayerAction.MOVE_LEFT);
        float y = axis(keys, PlayerAction.MOVE_FORWARD, PlayerAction.MOVE_BACKWARD);

        Vector2 movement = new Vector2(x, y);

        if (movement.len2() > 1) {
            movement.nor();
        }

        boolean skill = pressed.remove(keys.get(PlayerAction.USE_SKILL));
        boolean ultimate = pressed.remove(keys.get(PlayerAction.USE_ULTIMATE));
        boolean supportSkill = pressed.remove(keys.get(PlayerAction.USE_SUPPORT_SKILL));
        boolean supportUltimate = pressed.remove(keys.get(PlayerAction.USE_SUPPORT_ULTIMATE));

        return new PlayerCommand(movement, skill, ultimate, supportSkill, supportUltimate);
    }

    private float axis(Map<PlayerAction, Integer> keys, PlayerAction positive, PlayerAction negative) {
        return (held.contains(keys.get(positive)) ? 1 : 0) - (held.contains(keys.get(negative)) ? 1 : 0);
    }

    /**
     * @return 一時停止要求があった場合は一度だけ {@code true}
     */
    public boolean consumePause() {
        boolean result = pausePressed;
        pausePressed = false;

        return result;
    }

    /**
     * 押下中および未消費の入力状態をすべて破棄する。
     */
    public void clear() {
        held.clear();
        pressed.clear();
        pausePressed = false;
    }
}
