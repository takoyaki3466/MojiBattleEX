package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/** 一時分裂で生成する召喚個体1体の外観と通常攻撃能力。 */
public record SupportSplitMemberSpec(String displayGlyph, ResourceId texture, float attackDamage,
                                     float attackIntervalSeconds, float moveSpeed) {
    public SupportSplitMemberSpec {
        Objects.requireNonNull(displayGlyph, "displayGlyph");
        Objects.requireNonNull(texture, "texture");
        if (!Float.isFinite(attackDamage) || attackDamage < 0
                || !Float.isFinite(attackIntervalSeconds) || attackIntervalSeconds <= 0
                || !Float.isFinite(moveSpeed) || moveSpeed < 0) {
            throw new IllegalArgumentException("Invalid split member statistics");
        }
    }
}
