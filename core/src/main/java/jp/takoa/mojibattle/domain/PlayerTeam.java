package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

/**
 * 1人のプレイヤーが操作するメインキャラクターと補助キャラクターの組合せ。
 *
 * @param main 戦場で直接操作するキャラクター
 * @param support 自動能力と補助必殺を提供するキャラクター
 */
public record PlayerTeam(CharacterRuntime main, SupportCharacterRuntime support) {
}
