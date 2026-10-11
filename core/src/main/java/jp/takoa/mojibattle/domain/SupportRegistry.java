package jp.takoa.mojibattle.domain;

import java.util.*;

public final class SupportRegistry {
    private final Map<String,SupportCharacterDefinition> values=new LinkedHashMap<>();
    public void register(SupportCharacterDefinition value){if(values.putIfAbsent(value.id,value)!=null)throw new IllegalArgumentException("Duplicate support: "+value.id);}
    public SupportCharacterDefinition require(String id){var value=values.get(id);if(value==null)throw new IllegalArgumentException("Unknown support: "+id);return value;}
    public Collection<SupportCharacterDefinition> all(){return values.values();}
}
