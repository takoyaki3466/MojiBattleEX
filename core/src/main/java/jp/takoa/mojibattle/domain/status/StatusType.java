package jp.takoa.mojibattle.domain.status;

/** 戦闘計算へ適用可能な一時状態の分類。 */
public enum StatusType {
    ATTACK_UP,
    ATTACK_DOWN,
    DAMAGE_TAKEN_UP,
    MOVE_UP,
    MOVE_DOWN,
    CRITICAL_RATE_UP,
    CRITICAL_DAMAGE_UP,
    ULTIMATE_GAIN_UP,
    COOLDOWN_REDUCTION,
    BONUS_DAMAGE,
    STUN,
    BIND,
    BURN,
    HEAL_OVER_TIME
}
