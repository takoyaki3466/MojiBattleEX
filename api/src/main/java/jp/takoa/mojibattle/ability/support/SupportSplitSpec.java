package jp.takoa.mojibattle.ability.support;

import jp.takoa.mojibattle.battle.combatant.CombatantView;

import java.util.List;
import java.util.Objects;

/**
 * 一つの補助個体を複数の一時召喚へ分裂させる操作仕様。
 *
 * <h2>関係</h2>
 * <p>{@link SupportSplitMemberSpec}の不変リストと共通対象の{@link CombatantView}に依存する。
 * 補助Runtimeが構築し、{@link SupportActions#splitTemporarily(SupportSplitSpec)}へ渡し、ホストの召喚管理から参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SupportSplitSpec split = new SupportSplitSpec(
 *         List.of(leftMember, rightMember), 6f, context.target(), true, true);
 * context.supportActions().splitTemporarily(split);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>元の補助個体を隠すか、終了時に再結合するかをデータとして明示し、Runtimeに生成物の寿命管理を負わせないために作成された。</p>
 *
 * @param members 生成する一体以上の召喚個体仕様
 * @param durationSeconds 分裂を維持する正の秒数
 * @param commonTarget 全召喚へ設定する共通対象。未指定なら{@code null}
 * @param hideAndDisableOriginal 分裂中に元の補助個体を非表示・無効化するか
 * @param recombineOnEnd 終了時に元の補助個体を復帰させるか
 */
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
