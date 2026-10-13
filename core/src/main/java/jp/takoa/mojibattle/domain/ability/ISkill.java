package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

/** メインキャラクターが装備する再使用時間制のスキル契約。 */
public interface ISkill extends IAbility {
    /**
     * 発動条件を確認してスキルを開始する。
     *
     * @return 発動した場合は {@code true}
     */
    boolean tryActivate(BattleController battle, CharacterRuntime user);

    /** キャラクター生成時に再使用時間へ試合設定の倍率を適用する。 */
    default void initialize(CharacterRuntime user, float cooldownMultiplier) {
    }

    /** 必殺技からスキル効果だけを起動するための拡張点。 */
    default void activateFromUltimate(BattleController battle, CharacterRuntime user) {
    }
}
