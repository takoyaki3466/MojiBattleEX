package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

public interface ISkill {
    String id();
    boolean tryActivate(BattleController battle, CharacterRuntime user);
    default void initialize(CharacterRuntime user) {}
    default void activateFromUltimate(BattleController battle, CharacterRuntime user) {}
}
