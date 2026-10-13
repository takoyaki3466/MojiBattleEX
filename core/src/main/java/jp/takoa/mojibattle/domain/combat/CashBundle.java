package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/** 「㍻」が定期配置し、必殺技で起爆する札束オブジェクト。 */
public final class CashBundle extends PositionedCombatObject {
    public final PlayerId owner;
    public final String texturePath;
    private boolean detonated;

    public CashBundle(PlayerId owner, Vector3 position, String texturePath) {
        this.owner = owner;
        this.position.set(position);
        this.texturePath = texturePath;
    }

    public void detonate() {
        detonated = true;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean expired() {
        return detonated;
    }
}
