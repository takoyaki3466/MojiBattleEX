package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.status.StatusEffectSystem;
import jp.takoa.mojibattle.domain.status.StatusType;

/**
 * 対戦中のメインキャラクターについて、HP、ゲージ、行動不能時間、スキルストックを保持する可変状態。
 */
public final class CharacterRuntime {
    public final PlayerId playerId;
    public final CharacterDefinition definition;
    public final Vector3 position = new Vector3();
    public final Vector3 facing = new Vector3(0, 0, 1);
    private final float maxHp;
    private float hp;
    private float skillCooldown;
    private float skillCooldownDuration;
    private float ultimatePoints;
    private float totalDamage;
    private boolean immortal;
    private float stunRemaining;
    private float bindRemaining;
    private int skillStocks = 1;
    private int maximumSkillStocks = 1;
    private float stockRechargeSeconds;
    private float stockRechargeRemaining;
    private boolean awakened;
    private boolean zeroDefense;
    private float normalAttackCooldown;
    private float normalAttackIntervalMultiplier = 1f;
    private String normalAttackGlyph;
    private String normalAttackTexture;
    public final StatusEffectSystem statuses;

    public CharacterRuntime(PlayerId playerId, CharacterDefinition definition, Vector3 spawn) {
        this(playerId, definition, spawn, 1f);
    }

    /**
     * モード固有のHP倍率を反映した戦闘時状態を生成する。
     */
    public CharacterRuntime(PlayerId playerId, CharacterDefinition definition, Vector3 spawn, float healthMultiplier) {
        this.playerId = playerId;
        this.definition = definition;
        statuses = new StatusEffectSystem(playerId);
        position.set(spawn);
        maxHp = definition.maxHp * Math.max(0.01f, healthMultiplier);
        hp = maxHp;
        zeroDefense = definition.zeroDefense;
    }

    /**
     * クールタイム、行動不能、状態効果、スキルストック回復を1ステップ進める。
     * ストックは1フレームにつき最大1つだけ回復する。
     */
    public void update(float delta) {
        skillCooldown = Math.max(0, skillCooldown - delta);
        stunRemaining = Math.max(0, stunRemaining - delta);
        bindRemaining = Math.max(0, bindRemaining - delta);
        normalAttackCooldown = Math.max(0, normalAttackCooldown - delta);

        statuses.update(delta);

        if (skillStocks >= maximumSkillStocks || !(stockRechargeSeconds > 0)) {
            return;
        }
        stockRechargeRemaining -= delta;

        if (stockRechargeRemaining <= 0) {
            skillStocks++;
            stockRechargeRemaining = stockRechargeSeconds;
        }
    }

    public boolean isAlive() {
        return immortal || hp > 0;
    }

    public float hp() {
        return hp;
    }

    public float maxHp() {
        return maxHp;
    }

    public float hpRatio() {
        return hp / maxHp;
    }

    /**
     * @return 最大HPから現在HPを引いた非負の減少量
     */
    public float missingHp() {
        return Math.max(0, maxHp - hp);
    }

    /**
     * HPへダメージを反映し、実際に減少した量を返す。
     *
     * @return 無敵・戦闘不能状態を考慮した実ダメージ
     */
    public float receiveDamage(float damage) {
        if (immortal) {
            return Math.max(0, damage);
        }

        if (!isAlive()) {
            return 0;
        }

        float before = hp;
        hp = Math.max(0, hp - damage);

        return before - hp;
    }

    public float skillCooldown() {
        return skillCooldown;
    }

    public boolean skillReady() {
        return skillCooldown <= 0 && skillStocks > 0;
    }

    /**
     * 現在の待ち時間とHUDの充填率計算に使う基準時間を同時に設定する。
     */
    public void startSkillCooldown(float seconds) {
        skillCooldownDuration = Math.max(0, seconds);
        skillCooldown = skillCooldownDuration;
    }

    public void resetCooldown() {
        skillCooldown = 0;
    }

    /**
     * 現在の再使用時間とストック回復時間へ同じ非負倍率を適用する。
     */
    public void scaleSkillCooldown(float multiplier) {
        float safeMultiplier = Math.max(0, multiplier);

        skillCooldown = Math.max(0, skillCooldown * safeMultiplier);
        stockRechargeRemaining = Math.max(0, stockRechargeRemaining * safeMultiplier);
    }

    /**
     * @return HUD表示用に0から1へ収めたスキル充填率
     */
    public float skillChargeRatio() {
        if (skillStocks <= 0 && stockRechargeSeconds > 0) {
            return Math.max(0, Math.min(1, 1 - stockRechargeRemaining / stockRechargeSeconds));
        }

        return skillCooldownDuration <= 0 ? 1f : Math.max(0, Math.min(1, 1 - skillCooldown / skillCooldownDuration));
    }

    public float ultimatePoints() {
        return ultimatePoints;
    }

    public void addUltimatePoints(float points) {
        ultimatePoints = Math.min(100, Math.max(0, ultimatePoints + points));
    }

    /**
     * 必殺ポイントが不足していなければ指定量を消費する。
     *
     * @return 消費が成立した場合は {@code true}
     */
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

    public float totalDamage() {
        return totalDamage;
    }

    public void recordDamage(float damage) {
        totalDamage += Math.max(0, damage);
    }

    public void resetDamage() {
        totalDamage = 0;
    }

    public void setImmortal(boolean value) {
        immortal = value;
    }

    public void heal(float amount) {
        if (isAlive() && Float.isFinite(amount) && amount > 0) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    /**
     * HPが必ず残る場合に限り、指定量をコストとして消費する。
     */
    public boolean spendHealth(float amount) {
        if (!isAlive() || !Float.isFinite(amount) || amount <= 0 || hp <= amount) {
            return false;
        }

        hp -= amount;
        return true;
    }

    /**
     * 既存値より長い場合だけ行動不能時間を延長し、デバッグ表示用の状態効果も同期する。
     */
    public void applyStun(float seconds) {
        if (isAlive() && Float.isFinite(seconds) && seconds > 0) {
            stunRemaining = Math.max(stunRemaining, seconds);
            statuses.apply(new StatusEffect("stun", playerId.opponent(), StatusType.STUN, 1, seconds));
        }
    }

    public boolean isStunned() {
        return stunRemaining > 0;
    }

    public float stunRemaining() {
        return stunRemaining;
    }

    public void applyBind(float seconds) {
        if (Float.isFinite(seconds) && seconds > 0) {
            bindRemaining = Math.max(bindRemaining, seconds);
            statuses.apply(new StatusEffect("bind", playerId.opponent(), StatusType.BIND, 1, seconds));
        }
    }

    public boolean movementLocked() {
        return isStunned() || bindRemaining > 0;
    }

    /**
     * スキルストックの上限、初期値および1ストックの回復時間を設定する。
     */
    public void configureSkillStocks(int maximum, int initial, float recharge) {
        maximumSkillStocks = Math.max(1, maximum);
        skillStocks = Math.max(0, Math.min(maximumSkillStocks, initial));

        stockRechargeSeconds = Math.max(0, recharge);
        stockRechargeRemaining = stockRechargeSeconds;
    }

    /**
     * スキルストックを1つ消費し、空きが生じた時点から回復時計を開始する。
     *
     * @return 消費可能なストックがあった場合は {@code true}
     */
    public boolean consumeSkillStock() {
        if (skillStocks <= 0) {
            return false;
        }

        skillStocks--;

        if (skillStocks < maximumSkillStocks && stockRechargeRemaining <= 0) {
            stockRechargeRemaining = stockRechargeSeconds;
        }

        return true;
    }

    public int skillStocks() {
        return skillStocks;
    }

    public int maximumSkillStocks() {
        return maximumSkillStocks;
    }

    public void awaken() {
        awakened = true;
    }

    public boolean awakened() {
        return awakened;
    }

    /**
     * 編成パッシブを含め、このキャラクターを防御力0として固定する。
     */
    public void setZeroDefense(boolean value) {
        zeroDefense = value;
    }

    public boolean zeroDefense() {
        return zeroDefense;
    }

    /**
     * 行動不能タイマーと負の状態効果を同時に解除する。
     */
    public void clearNegativeStatuses() {
        stunRemaining = 0;
        bindRemaining = 0;
        statuses.clearNegative();
    }

    public boolean normalAttackReady() {
        return normalAttackCooldown <= 0;
    }

    /** 通常攻撃の次回発射までの時間を、現在の攻撃間隔倍率込みで設定する。 */
    public void resetNormalAttack(float baseInterval) {
        normalAttackCooldown = Math.max(0, baseInterval * normalAttackIntervalMultiplier);
    }

    /**
     * 通常攻撃の表示と間隔を上書きする。必殺技などによる戦闘中の永続変化に使用する。
     */
    public void enhanceNormalAttack(String glyph, String texture, float intervalMultiplier) {
        normalAttackGlyph = glyph;
        normalAttackTexture = texture;
        normalAttackIntervalMultiplier = Math.max(.1f, intervalMultiplier);
        normalAttackCooldown = Math.min(normalAttackCooldown, normalAttackIntervalMultiplier);
    }

    public String normalAttackGlyph() {
        return normalAttackGlyph == null ? definition.displayGlyph : normalAttackGlyph;
    }

    public String normalAttackTexture() {
        return normalAttackTexture == null ? definition.glyphTexture : normalAttackTexture;
    }
}
