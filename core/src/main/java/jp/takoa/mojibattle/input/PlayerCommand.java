package jp.takoa.mojibattle.input;

import com.badlogic.gdx.math.Vector2;

public record PlayerCommand(Vector2 movement, boolean useSkill, boolean useUltimate, boolean useSupportSkill, boolean useSupportUltimate) {
    public static PlayerCommand idle() { return new PlayerCommand(new Vector2(), false, false,false,false); }
}
