package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;

import java.util.List;

/**
 * 1プレイヤーが戦闘へ持ち込む代表メイン、追従メイン、補助キャラクターの不変編成。
 *
 * @param main プレイヤーが直接操作する代表メイン
 * @param subMains 代表へ追従して自動行動するメインキャラクター
 * @param supports 代表へ追従する補助キャラクター
 */
public record BattleTeamDefinition(
        CharacterDefinition main,
        List<CharacterDefinition> subMains,
        List<SupportCharacterDefinition> supports) {
    public BattleTeamDefinition {
        subMains = List.copyOf(subMains);
        supports = List.copyOf(supports);
    }
}
