package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.ability.AbilityStateLine;
import jp.takoa.mojibattle.ability.SupportPassiveRuntime;
import jp.takoa.mojibattle.battle.SupportNormalAttackModifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 補助キャラクターの位置、攻撃間隔、スキル再使用時間および独立した必殺ゲージを保持する。
 */
public final class SupportCharacterRuntime {
    public final PlayerId owner;
    public final SupportCharacterDefinition definition;
    public final String instanceId;
    final CoreSupportStateStore state = new CoreSupportStateStore();
    SupportPassiveRuntime passive;
    final List<TimedModifier> modifiers = new ArrayList<>();
    List<AbilityStateLine> actionStateLines = List.of();
    List<AbilityStateLine> passiveStateLines = List.of();
    public final Vector3 position = new Vector3();
    private float attackCooldown;
    private float skillCooldown;
    private float skillCooldownDuration;
    private float ultimatePoints;

    public SupportCharacterRuntime(PlayerId owner, SupportCharacterDefinition definition, Vector3 start) {
        this(owner, definition, start, owner.name().toLowerCase() + ":" + definition.id);
    }

    public SupportCharacterRuntime(PlayerId owner, SupportCharacterDefinition definition, Vector3 start, String instanceId) {
        this.owner = owner;
        this.definition = definition;
        this.instanceId = instanceId;
        position.set(start);
        attackCooldown = definition.attackInterval;
    }

    public void update(float delta, float ultimateChargeMultiplier) {
        attackCooldown = Math.max(0, attackCooldown - delta);
        skillCooldown = Math.max(0, skillCooldown - delta);
        ultimatePoints = Math.min(100, ultimatePoints + Math.max(0, delta) * definition.ultimateChargePerSecond * Math.max(0, ultimateChargeMultiplier));
        modifiers.removeIf(value -> value.tick(delta));
    }

    public boolean attackReady() {
        return attackCooldown <= 0;
    }

    public void resetAttack() {
        attackCooldown = definition.attackInterval;
    }

    public void resetAttack(float interval) { attackCooldown = Math.max(0.01f, interval); }

    public boolean skillReady() {
        return skillCooldown <= 0;
    }

    public void startSkillCooldown(float seconds) {
        skillCooldownDuration = Math.max(0, seconds);
        skillCooldown = skillCooldownDuration;
    }

    public float skillCooldown() {
        return skillCooldown;
    }

    public float skillChargeRatio() {
        return skillCooldownDuration <= 0 ? 1f : Math.max(0, Math.min(1, 1 - skillCooldown / skillCooldownDuration));
    }

    public void scaleSkillCooldown(float multiplier) {
        skillCooldown = Math.max(0, skillCooldown * Math.max(0, multiplier));
    }

    public float ultimatePoints() {
        return ultimatePoints;
    }

    public float ultimateChargeRatio() {
        return Math.min(1, ultimatePoints / Math.max(1, definition.ultimate.pointCost));
    }

    public void addUltimatePoints(float points) {
        ultimatePoints = Math.min(100, Math.max(0, ultimatePoints + points));
    }

    public boolean spendUltimate(float points) {
        if (points < 0 || ultimatePoints < points) {
            return false;
        }

        ultimatePoints -= points;
        return true;
    }

    public void maximizeUltimate() {
        ultimatePoints = 100;
    }

    record TimedModifier(SupportNormalAttackModifier modifier, float[] remaining) {
        boolean tick(float delta) { if (remaining == null) return false; remaining[0] -= delta; return remaining[0] <= 0; }
    }

    public void close() { modifiers.clear(); actionStateLines = List.of(); passiveStateLines = List.of(); state.close(); passive = null; }
    public jp.takoa.mojibattle.ability.SupportStateStore state() { return state; }
    public SupportPassiveRuntime passive() { return passive; }
    public void passive(SupportPassiveRuntime value) { passive = value; }
    public List<SupportNormalAttackModifier> normalAttackModifiers() { return modifiers.stream().map(TimedModifier::modifier).toList(); }
    public List<AbilityStateLine> stateLines() {
        if (passiveStateLines.isEmpty()) return actionStateLines;
        if (actionStateLines.isEmpty()) return passiveStateLines;
        List<AbilityStateLine> result = new ArrayList<>(passiveStateLines); result.addAll(actionStateLines); return List.copyOf(result);
    }
}
