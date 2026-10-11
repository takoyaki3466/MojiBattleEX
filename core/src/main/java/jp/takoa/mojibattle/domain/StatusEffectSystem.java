package jp.takoa.mojibattle.domain;

import java.util.*;

/** Timed modifiers. Reapplying the same source/id refreshes duration instead of stacking forever. */
public final class StatusEffectSystem {
    private final List<StatusEffect> effects=new ArrayList<>();
    private final PlayerId owner;
    public StatusEffectSystem(PlayerId owner){this.owner=owner;}
    public void apply(StatusEffect effect){
        effect.target=owner;StatusEffect current=effects.stream().filter(old->old.id.equals(effect.id)&&old.source==effect.source).findFirst().orElse(null);
        if(current==null||effect.stackRule==StatusStackRule.REPLACE){if(current!=null)effects.remove(current);effects.add(effect);return;}
        current.remaining=Math.max(current.remaining,effect.remaining);current.tickRemaining=Math.min(current.tickRemaining,effect.tickRemaining);
        if(effect.stackRule==StatusStackRule.STACK)current.stacks=Math.min(current.maximumStacks,current.stacks+1);
    }
    public void update(float delta){for(var e:effects)e.remaining-=delta;effects.removeIf(e->e.remaining<=0);}
    public float multiplier(StatusType type){float result=1f;for(var e:effects)if(e.type==type)result*=Math.pow(e.magnitude,e.stacks);return result;}
    public float additive(StatusType type){float result=0;for(var e:effects)if(e.type==type)result+=e.magnitude*e.stacks;return result;}
    public boolean has(StatusType type){return effects.stream().anyMatch(e->e.type==type);}
    public void clearNegative(){effects.removeIf(e->switch(e.type){case ATTACK_DOWN,DAMAGE_TAKEN_UP,MOVE_DOWN,STUN,BIND,BURN->true;default->false;});}
    public List<StatusEffect> active(){return List.copyOf(effects);}
}
