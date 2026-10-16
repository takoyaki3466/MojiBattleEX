package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.battle.CutInSpec;

import java.util.Objects;

/** HUDへ公開する現在のカットイン表示状態。 */
public final class CutInState {
    private final PlayerId player;
    private final CharacterRuntime source;
    private final CutInSpec spec;
    private float remainingSeconds;

    public CutInState(CharacterRuntime source, CutInSpec spec) {
        this.source = Objects.requireNonNull(source, "source");
        this.player = source.playerId;
        this.spec = Objects.requireNonNull(spec, "spec");
        remainingSeconds = spec.durationSeconds();
    }

    public void update(float deltaSeconds) {
        remainingSeconds = Math.max(0, remainingSeconds - Math.max(0, deltaSeconds));
    }

    public PlayerId player() {
        return player;
    }

    public CutInSpec spec() {
        return spec;
    }

    /** カメラが追跡する発動者。 */
    public CharacterRuntime source() {
        return source;
    }

    public float remainingSeconds() {
        return remainingSeconds;
    }

    /** 0（開始）から1（終了）までの演出進行率。 */
    public float progress() {
        return 1f - remainingSeconds / spec.durationSeconds();
    }

    public boolean expired() {
        return remainingSeconds <= 0;
    }
}
