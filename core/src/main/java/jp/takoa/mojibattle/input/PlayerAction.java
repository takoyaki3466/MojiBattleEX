package jp.takoa.mojibattle.input;

/** キー割当からゲーム内入力へ変換されるプレイヤー操作。 */
public enum PlayerAction {
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,
    USE_SKILL,
    USE_ULTIMATE,
    USE_SUPPORT_SKILL,
    USE_SUPPORT_ULTIMATE
}
