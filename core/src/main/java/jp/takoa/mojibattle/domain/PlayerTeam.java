package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

import java.util.List;

/**
 * 1人のプレイヤーが操作するメインキャラクターと補助キャラクターの組合せ。
 *
 * @param main     戦場で直接操作するキャラクター
 * @param subMains 代表へ追従してスキルと必殺技を自動発動するメインキャラクター
 * @param supports 自動能力と補助必殺を提供する補助キャラクター
 */
public record PlayerTeam(
        CharacterRuntime main,
        List<CharacterRuntime> subMains,
        List<SupportCharacterRuntime> supports) {
    public PlayerTeam {
        subMains = List.copyOf(subMains);
        supports = List.copyOf(supports);
    }

    /**
     * 旧来の単一補助参照が必要な表示・能力向けに先頭の補助を返す。
     */
    public SupportCharacterRuntime support() {
        return supports.isEmpty() ? null : supports.get(0);
    }
}
