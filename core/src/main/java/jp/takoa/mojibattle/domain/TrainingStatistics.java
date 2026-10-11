package jp.takoa.mojibattle.domain;

public final class TrainingStatistics {
    private float damage;
    private float lastDamage;
    private int hits;
    private float sampleSeconds;
    public void update(float delta) { sampleSeconds += Math.max(0, delta); }
    public void record(float value) { if (value > 0) { damage += value; lastDamage = value; hits++; } }
    public void reset() { damage = 0; lastDamage = 0; hits = 0; sampleSeconds = 0; }
    public float damage() { return damage; }
    public float lastDamage() { return lastDamage; }
    public int hits() { return hits; }
    public float dps() { return sampleSeconds > 0 ? damage / sampleSeconds : 0; }
    public float sampleSeconds() { return sampleSeconds; }
}
