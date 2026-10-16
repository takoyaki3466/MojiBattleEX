package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;

import java.util.Objects;

/** coreが所有する、固定ステップ時刻で一度だけ処理を発火する戦闘オブジェクト。 */
public final class ScheduledCombatAction extends PositionedCombatObject {
    private final Runnable action;
    private float remaining;
    private boolean fired;

    public ScheduledCombatAction(Vector3 position, float delaySeconds, Runnable action) {
        this.position.set(Objects.requireNonNull(position, "position"));
        if (!Float.isFinite(delaySeconds) || delaySeconds < 0) {
            throw new IllegalArgumentException("delaySeconds must be finite and non-negative");
        }
        this.remaining = delaySeconds;
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public void update(float delta) {
        if (fired) {
            return;
        }
        remaining -= Math.max(0, delta);
        if (remaining <= 0) {
            fired = true;
            action.run();
        }
    }

    @Override
    public boolean expired() {
        return fired;
    }
}
