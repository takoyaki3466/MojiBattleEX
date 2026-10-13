package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;

/**
 * 固有の戦場座標を持つ戦闘オブジェクトの最小共通実装。
 *
 * <p>寿司弾幕のような座標を必要としないタイマー型オブジェクトには使用しない。</p>
 */
public abstract non-sealed class PositionedCombatObject implements CombatObject {
    protected final Vector3 position = new Vector3();

    @Override
    public final Vector3 position() {
        return position;
    }
}
