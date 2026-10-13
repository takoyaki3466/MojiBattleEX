package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.PlayerId;

/** 補助キャラクターの位置、攻撃間隔、スキル再使用時間および独立した必殺ゲージを保持する。 */
public final class SupportCharacterRuntime {
    public final PlayerId owner;
    public final SupportCharacterDefinition definition;
    public final Vector3 position = new Vector3();
    private float attackCooldown;
    private float skillCooldown;
    private float skillCooldownDuration;
    private float ultimatePoints;

    public SupportCharacterRuntime(PlayerId owner, SupportCharacterDefinition definition, Vector3 start) {
        this.owner = owner;
        this.definition = definition;
        position.set(start);
        attackCooldown = definition.attackInterval;
    }

    public void update(float delta, float ultimateChargeMultiplier) {
        attackCooldown = Math.max(0, attackCooldown - delta);
        skillCooldown = Math.max(0, skillCooldown - delta);
        ultimatePoints = Math.min(
                100,
                ultimatePoints
                        + Math.max(0, delta)
                        * definition.ultimateChargePerSecond
                        * Math.max(0, ultimateChargeMultiplier));
    }

    public boolean attackReady() {
        return attackCooldown <= 0;
    }

    public void resetAttack() {
        attackCooldown = definition.attackInterval;
    }

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
        return skillCooldownDuration <= 0
                ? 1f
                : Math.max(0, Math.min(1, 1 - skillCooldown / skillCooldownDuration));
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
}
