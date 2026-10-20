package jp.takoa.mojibattle.domain.ability;

import java.util.Map;

/**
 * 設定可能なキャラクター能力が共有する公開DTO群。
 */
public final class AbilityConfigs {
    private AbilityConfigs() {
    }

    /**
     * 投射型スキルの弾、再使用時間、追加効果およびストック設定を保持するDTO。
     */
    public static final class ProjectileSkill {
        public String id;
        public String implementationId;
        public String nameKey;
        public String descriptionKey;
        public float cooldownSeconds;
        public float baseDamage;
        public float projectileSpeed;
        public float projectileLifetimeSeconds;
        public float projectileRadius;
        public float ultimatePointsPerHit;
        public float stunSeconds;
        public float bindSeconds;
        public float followUpDamage;
        public float followUpRadius;
        public float followUpSpeed;
        public float boomerangDistance;
        public float stockRechargeSeconds;
        public float healthCostRatio;
        public float buffAttackMultiplier;
        public float buffDefenseMultiplier;
        public float debuffAttackMultiplier;
        public float debuffDefenseMultiplier;
        public float buffDurationSeconds;
        public float activationEffectScale;
        public float fallingDamage;
        public float fallingRadius;
        public float fallingWarningSeconds;
        public float fallingEffectSeconds;
        public float fallingSpawnHeight;
        public float fallingHorizontalOffset;
        public boolean radial;
        public boolean pointsOnHit;
        public boolean boomerang;
        public int maximumStocks;
        public int initialStocks;
        public String type;
        public String followUpTexture;
        public String activationTexture;
        public String fallingObjectTexture;
        public String[] glyphs;
        public String[] textures;
        public float[][] offsets;
        public float[] damages;
        public float[] bindDurations;
        public int effectIndex;
        public String activationEffectId;
        public String projectileEffectId;
        public String hitEffectId;
        public Map<String, Object> parameters;
    }

    /**
     * 範囲・回復・召喚など複数方式で共有される必殺技設定DTO。
     */
    public static final class AreaUltimate {
        public String id;
        public String implementationId;
        public String nameKey;
        public String descriptionKey;
        public String dropStyle;
        public String type;
        public String objectTexture;
        public float pointCost;
        public float baseDamage;
        public float warningSeconds;
        public float effectSeconds;
        public float radius;
        public float spawnHeight;
        public float healAmount;
        public float lineLength;
        public float lineWidth;
        public float fallHorizontalOffset;
        public float burnDuration;
        public float burnDamage;
        public float burnTickInterval;
        public float burnUltimatePointsPerTick;
        public float movementSpeed;
        public float stunSeconds;
        public float barrageInterval;
        public float projectileSpeed;
        public float projectileLifetimeSeconds;
        public float projectileRadius;
        public float defenseDownMultiplier;
        public float defenseDownDuration;
        public boolean ignoreDefense;
        public float summonHp;
        public float summonDefense;
        public float summonAttackDamage;
        public float summonAttackInterval;
        public float summonProjectileSpeed;
        public int meteorCount;
        public int summonCount;
        public int directions;
        public int barrageCount;
        public String barrageSkillId;
        public int warningEffectIndex;
        public int impactEffectIndex;
        public String warningEffectId;
        public String impactEffectId;
        public String renderEffectId;
        public Map<String, Object> parameters;
    }

    /**
     * パッシブ種別と、戦闘計算へ供給する倍率・周期値を保持するDTO。
     */
    public static final class TimedPassive {
        public String id;
        public String implementationId;
        public String nameKey;
        public String descriptionKey;
        public String type;
        public float intervalSeconds;
        public float attackIncreasePerInterval;
        public float maximumMultiplier;
        public float familyBonusPerMember;
        public float moveSpeedMultiplier;
        public float skillCooldownMultiplier;
        public float ultimateGainMultiplier;
        public float incomingDamageMultiplier;
        public float hitboxMultiplier;
        public float stunMultiplier;
        public float ultimatePointsPerSecond;
        public float procChance;
        public float procStunSeconds;
        public float procInternalCooldown;
        public float missingHealthAttackBonus;
        public float ultimateDamageMultiplier;
        public String spawnObjectTexture;
        public int maximumSpawnedObjects;
        public Map<String, Object> parameters;
    }
}
