package jp.takoa.mojibattle.battle;

/**
 * カットイン中に使用するカメラワークの種類。
 *
 * <p>能力実装は具体的なカメラ座標ではなく演出意図を指定し、描画側が画面構成に
 * 合わせた軌道へ変換する。</p>
 */
public enum CutInMotion {
    /** 発動者へ急接近した後、斜めに回り込んで攻撃方向へ抜ける。 */
    SLASH_RUSH,
    /** 低い位置から横方向へ高速移動し、衝突直前に大きく引く。 */
    HORIZONTAL_CHARGE,
    /** 発動者を中心に回り込みながら寄り、最後に戦場全体を映す。 */
    ORBIT_RUSH
}
