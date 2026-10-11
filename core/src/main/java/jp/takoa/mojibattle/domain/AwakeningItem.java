package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class AwakeningItem implements CombatObject {
    private final Vector3 position=new Vector3(); private boolean consumed;
    public final String texturePath; public final float radius;
    public AwakeningItem(Vector3 position,String texturePath,float radius){this.position.set(position);this.texturePath=texturePath;this.radius=radius;}
    public void update(float delta){}
    public boolean expired(){return consumed;}
    public Vector3 position(){return position;}
    public void consume(){consumed=true;}
}
