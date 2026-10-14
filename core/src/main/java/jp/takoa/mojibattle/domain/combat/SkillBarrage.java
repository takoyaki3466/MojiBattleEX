package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 指定回数、一定間隔で同じスキルを発射する必殺技用のタイマーオブジェクト。
 */
public final class SkillBarrage extends PositionedCombatObject {
    public final PlayerId owner;
    public final String skillId;
    private final float interval;
    private int remaining;
    private float timer;
    private boolean pulse;

    public SkillBarrage(PlayerId owner, String skillId, int count, float interval, Vector3 position) {
        this.owner = owner;
        this.skillId = skillId;
        remaining = count;
        this.interval = interval;
        this.position.set(position);
        pulse = true;
    }

    @Override
    public void update(float delta) {
        if (remaining <= 0) {
            return;
        }
        timer -= delta;
        if (timer <= 0) {
            pulse = true;
            timer += interval;
            remaining--;
        }
    }

    /**
     * @return 未処理の発射タイミングがあった場合は {@code true}
     */
    public boolean takePulse() {
        boolean value = pulse;
        pulse = false;
        return value;
    }

    @Override
    public boolean expired() {
        return remaining <= 0 && !pulse;
    }

}
