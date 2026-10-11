package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class SkillBarrage implements CombatObject {
    public final PlayerId owner; public final String skillId; private final Vector3 position=new Vector3();
    private final float interval; private int remaining; private float timer; private boolean pulse;
    public SkillBarrage(PlayerId owner,String skillId,int count,float interval,Vector3 position){this.owner=owner;this.skillId=skillId;remaining=count;this.interval=interval;this.position.set(position);pulse=true;}
    public void update(float delta){if(remaining<=0)return;timer-=delta;if(timer<=0){pulse=true;timer+=interval;remaining--;}}
    public boolean takePulse(){boolean value=pulse;pulse=false;return value;}
    public boolean expired(){return remaining<=0&&!pulse;}
    public Vector3 position(){return position;}
}
