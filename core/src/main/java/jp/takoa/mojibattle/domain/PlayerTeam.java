package jp.takoa.mojibattle.domain;

import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

import java.util.List;

/**
 * 1人のプレイヤーが操作するメインキャラクターと補助キャラクターの組合せ。
 *
 * @param main     戦場で直接操作するキャラクター
 * @param supports 自動能力と補助必殺を提供する最大2体のキャラクター
 */
public record PlayerTeam(CharacterRuntime main, List<SupportCharacterRuntime> supports) {
    public PlayerTeam {
        supports = List.copyOf(supports);
    }

    /**
     * 旧来の単一補助参照が必要な表示・能力向けに先頭の補助を返す。
     */
    public SupportCharacterRuntime support() {
        return supports.isEmpty() ? null : supports.get(0);
    }
}
