package jp.takoa.mojibattle.input;

import com.badlogic.gdx.math.Vector2;

/**
 * 1フレーム分の移動量と各能力の発動要求をまとめた入力値。
 *
 * @param movement 二次元の移動入力
 * @param useSkill メインスキルの発動要求
 * @param useUltimate メイン必殺技の発動要求
 * @param useSupportSkill 補助スキルの発動要求
 * @param useSupportUltimate 補助必殺技の発動要求
 */
public record PlayerCommand(
        Vector2 movement,
        boolean useSkill,
        boolean useUltimate,
        boolean useSupportSkill,
        boolean useSupportUltimate) {

    /** @return 移動も能力発動も要求しないコマンド */
    public static PlayerCommand idle() {
        return new PlayerCommand(new Vector2(), false, false, false, false);
    }
}
