package jp.takoa.mojibattle.domain.status;

import jp.takoa.mojibattle.domain.PlayerId;

import java.util.ArrayList;
import java.util.List;

/**
 * 1キャラクターに付与された時間制状態を保持し、再付与時の更新規則と倍率計算を提供する。
 *
 * <p>同じ付与元・同じIDの効果は無制限に複製せず、指定された重複規則に従って更新する。</p>
 */
public final class StatusEffectSystem {
    private final List<StatusEffect> effects = new ArrayList<>();
    private final PlayerId owner;

    public StatusEffectSystem(PlayerId owner) {
        this.owner = owner;
    }

    /** 状態を付与し、同一効果が存在する場合は重複規則に従って更新する。 */
    public void apply(StatusEffect effect) {
        effect.target = owner;
        StatusEffect current = effects.stream()
                .filter(old -> old.id.equals(effect.id) && old.source == effect.source)
                .findFirst()
                .orElse(null);
        if (current == null || effect.stackRule == StatusStackRule.REPLACE) {
            if (current != null) {
                effects.remove(current);
            }
            effects.add(effect);
            return;
        }
        current.remaining = Math.max(current.remaining, effect.remaining);
        current.tickRemaining = Math.min(current.tickRemaining, effect.tickRemaining);
        if (effect.stackRule == StatusStackRule.STACK) {
            current.stacks = Math.min(current.maximumStacks, current.stacks + 1);
        }
    }

    public void update(float delta) {
        for (StatusEffect effect : effects) {
            effect.remaining -= delta;
        }
        effects.removeIf(effect -> effect.remaining <= 0);
    }

    public float multiplier(StatusType type) {
        float result = 1f;
        for (StatusEffect effect : effects) {
            if (effect.type == type) {
                result *= Math.pow(effect.magnitude, effect.stacks);
            }
        }
        return result;
    }

    public float additive(StatusType type) {
        float result = 0;
        for (StatusEffect effect : effects) {
            if (effect.type == type) {
                result += effect.magnitude * effect.stacks;
            }
        }
        return result;
    }

    public boolean has(StatusType type) {
        return effects.stream().anyMatch(effect -> effect.type == type);
    }

    /** 攻撃低下や行動不能など、回復対象として定義された負の状態だけを解除する。 */
    public void clearNegative() {
        effects.removeIf(effect -> switch (effect.type) {
            case ATTACK_DOWN, DAMAGE_TAKEN_UP, MOVE_DOWN, STUN, BIND, BURN -> true;
            default -> false;
        });
    }

    /** @return 現在の状態効果を変更不能なコピーとして返す */
    public List<StatusEffect> active() {
        return List.copyOf(effects);
    }
}
