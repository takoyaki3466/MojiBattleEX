package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.ability.SupportStateStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class CoreSupportStateStore implements SupportStateStore {
    private final Map<String, Object> values = new HashMap<>();
    private boolean closed;
    private Object get(String key) { requireOpen(); return values.get(Objects.requireNonNull(key)); }
    @Override public int getInt(String key, int fallback) { Object v=get(key); return v instanceof Integer n ? n : fallback; }
    @Override public void putInt(String key, int value) { put(key, value); }
    @Override public float getFloat(String key, float fallback) { Object v=get(key); return v instanceof Float n ? n : fallback; }
    @Override public void putFloat(String key, float value) { if (!Float.isFinite(value)) throw new IllegalArgumentException("value"); put(key,value); }
    @Override public boolean getBoolean(String key, boolean fallback) { Object v=get(key); return v instanceof Boolean b ? b : fallback; }
    @Override public void putBoolean(String key, boolean value) { put(key,value); }
    @Override public String getString(String key, String fallback) { Object v=get(key); return v instanceof String s ? s : fallback; }
    @Override public void putString(String key, String value) { put(key, Objects.requireNonNull(value)); }
    @Override public boolean contains(String key) { return get(key) != null; }
    @Override public void remove(String key) { requireOpen(); values.remove(Objects.requireNonNull(key)); }
    private void put(String key,Object value){ requireOpen(); values.put(Objects.requireNonNull(key),value); }
    void close(){ values.clear(); closed=true; }
    private void requireOpen(){ if(closed) throw new IllegalStateException("Support state belongs to a finished battle"); }
}
