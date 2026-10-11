package jp.takoa.mojibattle.config;

public final class ControlConfig {
    public Binding playerOne;
    public Binding playerTwo;
    public Binding playerThree;
    public Binding playerFour;
    public String pause;

    public static final class Binding {
        public String forward, backward, left, right, skill, ultimate, supportSkill, supportUltimate;
    }
}
