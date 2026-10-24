package jp.takoa.mojibattle.config;

/**
 * 最大4人分の操作割当と共通の一時停止キーを保持する設定DTO。
 */
public final class ControlConfig {
    public Binding playerOne;
    public Binding playerTwo;
    public Binding playerThree;
    public Binding playerFour;
    public String pause;

    /**
     * 1人のプレイヤーが使用する移動・能力発動キーの設定。
     */
    public static final class Binding {
        public String forward;
        public String backward;
        public String left;
        public String right;
        public String skill;
        public String ultimate;
        public String supportSkill;
        public String supportUltimate;
    }
}
