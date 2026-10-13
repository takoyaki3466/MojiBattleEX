package jp.takoa.mojibattle.config;

/** 戦闘時間、物理判定、終盤補正など、試合全体で共有する設定DTO。 */
public final class BattleConfig {
    public float durationSeconds;
    public float fixedStepSeconds;
    public float finalMinuteSeconds;
    public float finalMinuteAttackMultiplier;
    public float arenaHalfExtent;
    public float gravity;
    public float groundY;
    public float playerCollisionRadius;
    public float skillCooldownMultiplier;
    public float healthMultiplier;
}
