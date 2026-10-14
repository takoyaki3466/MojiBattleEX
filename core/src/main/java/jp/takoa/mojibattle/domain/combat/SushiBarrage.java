package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * 寿司弾幕の発射回数と発射間隔、および各弾へ渡す攻撃設定を保持する。
 */
public final class SushiBarrage implements CombatObject {
    public final PlayerId owner;
    public final int count;
    public final float interval;
    public final float damage;
    public final float speed;
    public final float lifetime;
    public final float radius;
    public final String texture;
    public final String effectId;
    public final boolean ignoreDefense;
    private int fired;
    private float timer;

    public SushiBarrage(PlayerId owner, int count, float interval, float damage, float speed, float lifetime, float radius, String texture, String effectId, boolean ignoreDefense) {
        this.owner = owner;
        this.count = count;
        this.interval = interval;
        this.damage = damage;
        this.speed = speed;
        this.lifetime = lifetime;
        this.radius = radius;
        this.texture = texture;
        this.effectId = effectId;
        this.ignoreDefense = ignoreDefense;
    }

    @Override
    public void update(float delta) {
        timer -= delta;
    }

    /**
     * @return 次の弾を発射すべき場合は {@code true}
     */
    public boolean takeShot() {
        if (fired >= count || timer > 0) {
            return false;
        }

        fired++;
        timer += interval;

        return true;
    }

    public int shotIndex() {
        return Math.max(0, fired - 1);
    }

    @Override
    public boolean expired() {
        return fired >= count && timer <= 0;
    }

    @Override
    public Vector3 position() {
        return Vector3.Zero;
    }
}
