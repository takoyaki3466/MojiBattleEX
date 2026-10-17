package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatantView;

import java.util.List;
import java.util.Objects;

/** 補助を複数の一時召喚へ分裂させる設定。 */
public record SupportSplitSpec(List<SupportSplitMemberSpec> members, float durationSeconds,
                               CombatantView commonTarget, boolean hideAndDisableOriginal,
                               boolean recombineOnEnd) {
    public SupportSplitSpec {
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        if (members.isEmpty() || members.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Split requires at least one member");
        }
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds");
        }
    }
}
