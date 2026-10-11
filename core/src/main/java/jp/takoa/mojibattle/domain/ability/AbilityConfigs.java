package jp.takoa.mojibattle.domain.ability;

/** Typed schemas shared by the configurable character abilities. */
public final class AbilityConfigs {
    private AbilityConfigs() {}
    public static final class ProjectileSkill {
        public String id;
        public float cooldownSeconds, baseDamage, projectileSpeed, projectileLifetimeSeconds, projectileRadius, ultimatePointsPerHit;
        public float stunSeconds, bindSeconds, followUpDamage, followUpRadius, followUpSpeed, boomerangDistance, stockRechargeSeconds, healthCostRatio;
        public boolean radial, pointsOnHit, boomerang;
        public int maximumStocks, initialStocks;
        public String type, followUpTexture;
        public String[] glyphs;
        public String[] textures;
        public float[][] offsets;
        public float[] damages,bindDurations;
        public int effectIndex;
    }
    public static final class AreaUltimate {
        public String id;
        public String nameKey;
        public String dropStyle;
        public String type;
        public String objectTexture;
        public float pointCost, baseDamage, warningSeconds, effectSeconds, radius, spawnHeight;
        public float healAmount, lineLength, lineWidth, fallHorizontalOffset, burnDuration, burnDamage, burnTickInterval, burnUltimatePointsPerTick;
        public float movementSpeed, stunSeconds, barrageInterval;
        public float summonHp, summonAttackDamage, summonAttackInterval, summonProjectileSpeed;
        public int meteorCount, summonCount, directions, barrageCount;
        public String barrageSkillId;
        public int warningEffectIndex, impactEffectIndex;
    }
    public static final class TimedPassive {
        public String id;
        public String type;
        public float intervalSeconds, attackIncreasePerInterval, maximumMultiplier;
        public float familyBonusPerMember;
        public float moveSpeedMultiplier, skillCooldownMultiplier, ultimateGainMultiplier;
        public float incomingDamageMultiplier, hitboxMultiplier, stunMultiplier, ultimatePointsPerSecond;
        public float procChance, procStunSeconds, procInternalCooldown, missingHealthAttackBonus;
    }
}
