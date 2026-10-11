package jp.takoa.mojibattle.domain;

public final class StatusEffect {
    public final String id;
    public final PlayerId source;
    public PlayerId target;
    public final StatusType type;
    public final float magnitude;
    public final StatusStackRule stackRule;
    public final int maximumStacks;
    public int stacks=1;
    public float remaining, tickRemaining, tickInterval=1f, ultimatePointsPerTick;
    public StatusEffect(String id,PlayerId source,StatusType type,float magnitude,float duration){this(id,source,type,magnitude,duration,StatusStackRule.REFRESH,1);}
    public StatusEffect(String id,PlayerId source,StatusType type,float magnitude,float duration,StatusStackRule stackRule,int maximumStacks){this.id=id;this.source=source;this.type=type;this.magnitude=magnitude;this.stackRule=stackRule;this.maximumStacks=Math.max(1,maximumStacks);remaining=duration;tickRemaining=1f;}
    public StatusEffect tickEvery(float seconds){tickInterval=Math.max(.01f,seconds);tickRemaining=tickInterval;return this;}
    public StatusEffect ultimatePointsPerTick(float points){ultimatePointsPerTick=Math.max(0,points);return this;}
}
