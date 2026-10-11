package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.config.FieldEffectDefinition;
import java.util.*;

/** Deduplicates field effects by ID and exposes non-destructive calculated modifiers. */
public final class FieldEffectSystem {
    private final Map<String,FieldEffectDefinition> active=new LinkedHashMap<>();
    public void activate(FieldEffectDefinition effect){active.putIfAbsent(effect.id,effect);}
    public Collection<FieldEffectDefinition> active(){return active.values();}
    public boolean hasType(String type){return active.values().stream().anyMatch(e->e.type.equals(type));}
    public float multiplier(String type,float elapsed){float result=1f;for(var e:active.values())if(e.type.equals(type)){float value=e.magnitude;if(e.intervalSeconds>0)value=Math.min(e.maximum,(float)Math.floor(elapsed/e.intervalSeconds)*e.magnitude);result*=type.equals("attack_growth")?1f+value:value;}return result;}
    public float additive(String type){float result=0;for(var e:active.values())if(e.type.equals(type))result+=e.magnitude;return result;}
    public float timedMultiplier(String type,float elapsed){float result=1f;for(var e:active.values())if(e.type.equals(type)){
        boolean activeNow=e.intervalSeconds<=0||e.durationSeconds<=0||elapsed%e.intervalSeconds<e.durationSeconds;if(activeNow)result*=e.magnitude;
    }return result;}
    public float interval(String type,float fallback){for(var e:active.values())if(e.type.equals(type)&&e.intervalSeconds>0)return e.intervalSeconds;return fallback;}
    public List<String> ids(){return List.copyOf(active.keySet());}
}
