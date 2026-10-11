package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;

public interface IUltimate {
    String id();
    boolean tryActivate(BattleController battle, CharacterRuntime user);
}
