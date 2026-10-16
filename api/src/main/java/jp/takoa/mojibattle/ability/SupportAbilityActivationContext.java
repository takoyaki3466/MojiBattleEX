package jp.takoa.mojibattle.ability;

import jp.takoa.mojibattle.battle.CombatActions;
import jp.takoa.mojibattle.battle.CombatantView;

import java.util.Objects;

/**
 * 外部補助能力へ渡す、core型を含まない実行コンテキスト。
 * @param support 発動する補助キャラクター
 * @param owner 補助キャラクターを所有するメイン
 * @param target 現在の敵対象。存在しない場合は{@code null}
 * @param actions 所属戦闘だけを操作できる境界
 * @param config Ability固有のJSON設定
 * @param battleElapsedSeconds 戦闘開始からの固定ステップ経過秒
 */
public record SupportAbilityActivationContext(SupportView support, CombatantView owner,
                                              CombatantView target, CombatActions actions,
                                              ConfigView config, float battleElapsedSeconds) {
    public SupportAbilityActivationContext {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(config, "config");
        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }
    }
}
