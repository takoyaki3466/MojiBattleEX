package jp.takoa.mojibattle.domain.combat;

import jp.takoa.mojibattle.battle.action.spec.BattlefieldPulseSpec;
import jp.takoa.mojibattle.domain.CharacterRuntime;

import java.util.Objects;

/** 中央吸引と周期ダメージを継続する戦場オブジェクト。 */
public final class BattlefieldPulse extends PositionedCombatObject {
    private final CharacterRuntime owner;
    private final BattlefieldPulseSpec spec;
    private float remainingSeconds;
    private float tickRemaining;
    private boolean damageTick;

    public BattlefieldPulse(CharacterRuntime owner, BattlefieldPulseSpec spec) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.spec = Objects.requireNonNull(spec, "spec");
        remainingSeconds = spec.durationSeconds();
        tickRemaining = spec.tickIntervalSeconds();
    }

    @Override
    public void update(float deltaSeconds) {
        float delta = Math.max(0, deltaSeconds);
        remainingSeconds = Math.max(0, remainingSeconds - delta);
        tickRemaining -= delta;
        if (tickRemaining <= 0) {
            damageTick = true;
            tickRemaining += spec.tickIntervalSeconds();
        }
    }

    public boolean takeDamageTick() {
        boolean value = damageTick;
        damageTick = false;
        return value;
    }

    public CharacterRuntime owner() {
        return owner;
    }

    public BattlefieldPulseSpec spec() {
        return spec;
    }

    @Override
    public boolean expired() {
        return remainingSeconds <= 0;
    }
}
