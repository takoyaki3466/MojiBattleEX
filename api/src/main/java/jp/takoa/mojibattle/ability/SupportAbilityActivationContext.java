package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;

import java.util.Objects;

/**
 * 外部補助能力へ渡す、core型を含まない実行コンテキスト。
 */
public final class SupportAbilityActivationContext {
    private final SupportView support;
    private final CombatantView owner;
    private final CombatantView target;
    private final CombatActions actions;
    private final ConfigView config;
    private final float battleElapsedSeconds;
    private final SupportActions supportActions;
    private final SupportStateStore state;

    public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target,
            CombatActions actions, ConfigView config, float battleElapsedSeconds) {
        this(support, owner, target, actions, config, battleElapsedSeconds, null, null);
    }

    public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target,
            CombatActions actions, ConfigView config, float battleElapsedSeconds,
            SupportActions supportActions, SupportStateStore state) {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(config, "config");
        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
        this.support = support; this.owner = owner; this.target = target; this.actions = actions;
        this.config = config; this.battleElapsedSeconds = battleElapsedSeconds;
        this.supportActions = supportActions; this.state = state;
    }
    public SupportView support() { return support; }
    public CombatantView owner() { return owner; }
    public CombatantView target() { return target; }
    public CombatActions actions() { return actions; }
    public ConfigView config() { return config; }
    public float battleElapsedSeconds() { return battleElapsedSeconds; }
    public SupportActions supportActions() { return Objects.requireNonNull(supportActions, "Host does not provide SupportActions"); }
    public SupportStateStore state() { return Objects.requireNonNull(state, "Host does not provide SupportStateStore"); }
}
