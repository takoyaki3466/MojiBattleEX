package jp.takoa.mojibattle.domain.combat;

import jp.takoa.mojibattle.battle.DomainSpec;
import jp.takoa.mojibattle.domain.CharacterRuntime;

import java.util.Objects;

/** 発動者ごとに独立して残存時間を持つ、重複可能な領域インスタンス。 */
public final class ActiveDomain {
    private final CharacterRuntime owner;
    private final DomainSpec spec;
    private float remainingSeconds;

    public ActiveDomain(CharacterRuntime owner, DomainSpec spec) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.spec = Objects.requireNonNull(spec, "spec");
        remainingSeconds = spec.durationSeconds();
    }

    public void update(float deltaSeconds) {
        remainingSeconds = Math.max(0, remainingSeconds - Math.max(0, deltaSeconds));
    }

    public CharacterRuntime owner() {
        return owner;
    }

    public DomainSpec spec() {
        return spec;
    }

    public float remainingSeconds() {
        return remainingSeconds;
    }

    public boolean expired() {
        return remainingSeconds <= 0;
    }
}
