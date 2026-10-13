package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

/** メインキャラクターが必殺ポイントを消費して発動する必殺技契約。 */
public interface IUltimate extends IAbility {
    /** @return 必要ポイントを満たして発動した場合は {@code true} */
    boolean tryActivate(BattleController battle, CharacterRuntime user);
}
