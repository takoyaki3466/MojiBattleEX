package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.event.EventBus;
import jp.takoa.mojibattle.event.gameplay.CharacterDiedEvent;
import jp.takoa.mojibattle.event.gameplay.DamageEvent;

/**
 * 攻撃・防御倍率を適用し、実ダメージを対象と攻撃者の統計へ反映する。
 */
public final class DamageService {
    private final EventBus gameplayEvents;

    public DamageService() {
        this(null);
    }

    /**
     * ダメージ確定イベントを配送する任意のゲームイベントバスを受け取る。
     */
    public DamageService(EventBus gameplayEvents) {
        this.gameplayEvents = gameplayEvents;
    }

    /**
     * 攻撃ごとに切り替え可能なダメージ計算特性。
     */
    public record AttackTraits(boolean ignoreDefense) {
        public static final AttackTraits STANDARD = new AttackTraits(false);
    }

    public float apply(CharacterRuntime attacker, CharacterRuntime target, float baseDamage, float attackMultiplier, float defenseMultiplier) {
        return apply(attacker, target, baseDamage, attackMultiplier, defenseMultiplier, AttackTraits.STANDARD);
    }

    /**
     * 検証済みの倍率から要求ダメージを算出し、対象へ適用できた実値を返す。
     *
     * @throws IllegalArgumentException 数値が負数または有限値でない場合
     */
    public float apply(CharacterRuntime attacker, CharacterRuntime target, float baseDamage, float attackMultiplier, float defenseMultiplier, AttackTraits traits) {
        return apply(attacker, target, baseDamage, attackMultiplier, defenseMultiplier, 1f, traits);
    }

    /**
     * 防御倍率と、防御無視の影響を受けない被ダメージ補正を分離して適用する。
     * 状態・フィールド由来の被ダメージ補正は、防御無視攻撃にも適用される。
     *
     * @throws IllegalArgumentException 数値が負数または有限値でない場合
     */
    public float apply(CharacterRuntime attacker, CharacterRuntime target, float baseDamage, float attackMultiplier, float defenseMultiplier, float incomingEffectMultiplier, AttackTraits traits) {
        validate(baseDamage, "baseDamage");
        validate(attackMultiplier, "attackMultiplier");
        validate(defenseMultiplier, "defenseMultiplier");
        validate(incomingEffectMultiplier, "incomingEffectMultiplier");

        if (!target.isAlive()) {
            return 0;
        }

        float requested = baseDamage * attackMultiplier * (traits.ignoreDefense() ? 1f : defenseMultiplier) * incomingEffectMultiplier;

        if (!Float.isFinite(requested) || requested < 0) {
            throw new IllegalArgumentException("Invalid calculated damage");
        }

        float applied = target.receiveDamage(requested);

        attacker.recordDamage(applied);

        if (gameplayEvents != null && applied > 0) {
            gameplayEvents.post(new DamageEvent(attacker, target, applied));

            if (!target.isAlive()) {
                gameplayEvents.post(new CharacterDiedEvent(target));
            }
        }
        return applied;
    }

    private static void validate(float value, String name) {
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
