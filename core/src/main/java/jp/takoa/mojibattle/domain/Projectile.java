package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public final class Projectile implements CombatObject {
    public final long attackId;
    public final PlayerId owner;
    public final String glyph;
    public final String texturePath;
    public final int effectIndex;
    public final Vector3 velocity = new Vector3();
    public final float radius;
    public final float baseDamage;
    public final float stunSeconds;
    public final float bindSeconds;
    public final boolean boomerang;
    public final float boomerangDistance;
    public final float ultimatePointsOnHit;
    private final Vector3 position = new Vector3();
    private final Vector3 origin = new Vector3();
    private float remainingLife;
    private boolean consumed;
    private boolean returning;
    private float hitCooldown;
    private float curveStrength;
    private boolean hasCriticalProfile, criticalEnabled;
    private float criticalRate, criticalDamageMultiplier;
    private String followUpGlyph, followUpTexture;
    private float followUpDamage, followUpRadius, followUpSpeed;

    public Projectile(long attackId, PlayerId owner, String glyph, String texturePath, int effectIndex, Vector3 start, Vector3 velocity,
                      float radius, float baseDamage, float stunSeconds, float life) {
        this.attackId = attackId; this.owner = owner; this.glyph = glyph; this.texturePath = texturePath; this.effectIndex = effectIndex; this.position.set(start);
        this.velocity.set(velocity); this.radius = radius; this.baseDamage = baseDamage;
        this.stunSeconds = stunSeconds; this.bindSeconds=0; this.boomerang=false;this.boomerangDistance=0;this.ultimatePointsOnHit=0;this.remainingLife = life;origin.set(start);
    }
    public Projectile(long attackId, PlayerId owner, String glyph, String texturePath, int effectIndex, Vector3 start, Vector3 velocity,
                      float radius,float baseDamage,float stunSeconds,float bindSeconds,float life,boolean boomerang,float boomerangDistance,float pointsOnHit){
        this.attackId=attackId;this.owner=owner;this.glyph=glyph;this.texturePath=texturePath;this.effectIndex=effectIndex;position.set(start);origin.set(start);
        this.velocity.set(velocity);this.radius=radius;this.baseDamage=baseDamage;this.stunSeconds=stunSeconds;this.bindSeconds=bindSeconds;this.remainingLife=life;
        this.boomerang=boomerang;this.boomerangDistance=boomerangDistance;this.ultimatePointsOnHit=pointsOnHit;
    }
    public void update(float delta) { if(curveStrength!=0)velocity.rotate(Vector3.Y,curveStrength*delta);position.mulAdd(velocity, delta); hitCooldown=Math.max(0,hitCooldown-delta);if(boomerang&&!returning&&position.dst2(origin)>=boomerangDistance*boomerangDistance){returning=true;velocity.scl(-1);}if(returning&&position.dst2(origin)<.5f)consumed=true;remainingLife -= delta; }
    public boolean expired() { return consumed || remainingLife <= 0; }
    public Vector3 position() { return position; }
    public void consume() { consumed = true; }
    public boolean persistentOnHit(){return boomerang;}
    public boolean hitReady(){return hitCooldown<=0;}
    public void markHit(){hitCooldown=.32f;}
    public Projectile curve(float degreesPerSecond){curveStrength+=degreesPerSecond;return this;}
    public Projectile criticalProfile(boolean enabled,float rate,float damageMultiplier){hasCriticalProfile=true;criticalEnabled=enabled;criticalRate=rate;criticalDamageMultiplier=damageMultiplier;return this;}
    public boolean hasCriticalProfile(){return hasCriticalProfile;}
    public boolean criticalEnabled(){return criticalEnabled;}
    public float criticalRate(){return criticalRate;}
    public float criticalDamageMultiplier(){return criticalDamageMultiplier;}
    public Projectile followUp(String glyph,String texture,float damage,float radius,float speed){
        followUpGlyph=glyph;followUpTexture=texture;followUpDamage=damage;followUpRadius=radius;followUpSpeed=speed;return this;
    }
    public boolean hasFollowUp(){return followUpTexture!=null;}
    public String followUpGlyph(){return followUpGlyph;}
    public String followUpTexture(){return followUpTexture;}
    public float followUpDamage(){return followUpDamage;}
    public float followUpRadius(){return followUpRadius;}
    public float followUpSpeed(){return followUpSpeed;}
}
