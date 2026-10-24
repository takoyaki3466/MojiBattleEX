package jp.takoa.mojibattle.ability.context;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.support.SupportActions;
import jp.takoa.mojibattle.ability.support.SupportStateStore;
import jp.takoa.mojibattle.ability.support.SupportView;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.battle.action.CombatActions;

import java.util.Objects;

/**
 * 補助パッシブの全ライフサイクルで共有される、戦闘個体単位のコンテキスト。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.runtime.SupportPassiveRuntime}の全コールバックから参照される。
 * {@link SupportView}、{@link CombatantView}、{@link CombatActions}、{@link SupportActions}、
 * {@link SupportStateStore}、{@link ConfigView}を一つに束ね、ホストの補助コントローラーが生成する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * int stacks = context.state().getInt("stacks", 0);
 * context.supportActions().setStateLines(List.of(
 *         AbilityStateLine.of("mod.support.stacks", 10, stacks)));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>各コールバックへ同じ引数群を重複して渡さず、補助個体の状態と操作権限を取り違えないようにするために作成された。</p>
 *
 * @param support              パッシブを所有する補助個体
 * @param owner                補助を編成したメイン戦闘個体
 * @param target               現在の対象。存在しない場合は{@code null}
 * @param actions              一般戦闘操作
 * @param supportActions       補助個体専用操作
 * @param state                この補助個体専用の状態ストア
 * @param config               登録時に与えられた設定
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record SupportPassiveContext(SupportView support, CombatantView owner, CombatantView target,
                                    CombatActions actions, SupportActions supportActions, SupportStateStore state,
                                    ConfigView config, float battleElapsedSeconds) {

    public SupportPassiveContext {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(supportActions, "supportActions");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(config, "config");

        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
    }
}
