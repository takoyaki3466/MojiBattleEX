package jp.takoa.mojibattle.domain;

public final class DamageService {
    public float apply(CharacterRuntime attacker, CharacterRuntime target, float baseDamage, float attackMultiplier, float defenseMultiplier) {
        validate(baseDamage, "baseDamage");
        validate(attackMultiplier, "attackMultiplier");
        validate(defenseMultiplier, "defenseMultiplier");
        if (!target.isAlive()) return 0;
        float requested = baseDamage * attackMultiplier * defenseMultiplier;
        if (!Float.isFinite(requested) || requested < 0) throw new IllegalArgumentException("Invalid calculated damage");
        float applied = target.receiveDamage(requested);
        attacker.recordDamage(applied);
        return applied;
    }

    private static void validate(float value, String name) {
        if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException(name + " must be finite and non-negative");
    }
}
