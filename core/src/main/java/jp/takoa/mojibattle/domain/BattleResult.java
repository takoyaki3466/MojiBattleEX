package jp.takoa.mojibattle.domain;

public record BattleResult(PlayerId winner, Reason reason) {
    public enum Reason { KNOCKOUT, TIMEOUT, DRAW }
    public boolean draw() { return winner == null; }
}
