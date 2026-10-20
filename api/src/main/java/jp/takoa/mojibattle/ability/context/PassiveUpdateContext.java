package jp.takoa.mojibattle.ability.context;

import jp.takoa.mojibattle.battle.action.CombatActions;
import jp.takoa.mojibattle.battle.combatant.CombatantView;

import java.util.Objects;

/**
 * 戦闘の固定ステップごとにパッシブへ渡される不変コンテキスト。
 * ポーズ中および勝敗確定後は生成されない。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.runtime.PassiveRuntime#update(PassiveUpdateContext)}の入力であり、
 * 所有者は{@link CombatantView}、副作用は{@link CombatActions}で表す。ホストの固定更新ループから依存される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * elapsed += context.deltaSeconds();
 * if (elapsed >= 1f) context.actions().heal(context.owner(), 1f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>描画フレーム速度ではなく決定的な戦闘ステップで経過処理を行い、Runtimeがホストの時計へ直接依存しないようにする。</p>
 *
 * @param owner このRuntimeを所有する参加者
 * @param actions 所属戦闘だけを操作できる境界
 * @param deltaSeconds 今回の固定ステップ秒
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record PassiveUpdateContext(CombatantView owner, CombatActions actions, float deltaSeconds,
                                   float battleElapsedSeconds) {
    public PassiveUpdateContext {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }
        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
    }
}
