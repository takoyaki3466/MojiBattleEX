package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.CombatActions;

import java.util.Objects;

/** 補助パッシブの全ライフサイクルで共有される個体コンテキスト。 */
public record SupportPassiveContext(SupportView support, CombatantView owner, CombatantView target,
                                    CombatActions actions, SupportActions supportActions, SupportStateStore state,
                                    ConfigView config, float battleElapsedSeconds) {
    public SupportPassiveContext {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(supportActions, "supportActions");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(config, "config");
        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
    }
}
