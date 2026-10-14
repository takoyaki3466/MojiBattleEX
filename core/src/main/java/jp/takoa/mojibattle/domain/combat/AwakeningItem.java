package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;

/**
 * 戦場に出現し、プレイヤーが取得するまで位置と描画情報を保持する覚醒アイテム。
 */
public final class AwakeningItem extends PositionedCombatObject {
    private boolean consumed;
    public final String texturePath;
    public final float radius;

    public AwakeningItem(Vector3 position, String texturePath, float radius) {
        this.position.set(position);
        this.texturePath = texturePath;
        this.radius = radius;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean expired() {
        return consumed;
    }

    /**
     * 取得済みとして印を付け、次の戦場更新で除去可能にする。
     */
    public void consume() {
        consumed = true;
    }
}
