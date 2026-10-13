package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/** 警告時間の後に直線範囲へ一度だけダメージを発生させる戦場オブジェクト。 */
public final class LineStrike extends PositionedCombatObject {
    public final PlayerId owner;
    public final Vector3 direction = new Vector3();
    public final float length, width, baseDamage;
    public final int warningEffectIndex, impactEffectIndex;
    public final String objectTexture;
    private final float warningDuration, effectDuration;
    private float elapsed;
    private boolean damagePending;
    private boolean mainUltimate;

    public LineStrike(PlayerId owner, Vector3 start, Vector3 direction, float length, float width, float damage,
                      float warning, float effectDuration, int warningEffectIndex, int impactEffectIndex, String objectTexture) {
        this.owner=owner; position.set(start); this.direction.set(direction).set(direction.x,0,direction.z).nor();
        this.length=length; this.width=width; baseDamage=damage; warningDuration=warning; this.effectDuration=effectDuration;
        this.warningEffectIndex=warningEffectIndex; this.impactEffectIndex=impactEffectIndex; this.objectTexture=objectTexture;
    }
    @Override public void update(float delta) { float before=elapsed; elapsed+=delta; if(before<warningDuration&&elapsed>=warningDuration) damagePending=true; }
    public boolean takeDamagePending(){boolean result=damagePending;damagePending=false;return result;}
    public boolean impacting(){return elapsed>=warningDuration;}
    public LineStrike mainUltimate(){mainUltimate=true;return this;}
    public boolean isMainUltimate(){return mainUltimate;}
    public float warningProgress(){return warningDuration<=0?1:Math.min(1,elapsed/warningDuration);}
    @Override public boolean expired(){return elapsed>=warningDuration+effectDuration;}
}
