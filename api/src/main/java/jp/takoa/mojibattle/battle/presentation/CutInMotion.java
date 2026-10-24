package jp.takoa.mojibattle.battle.presentation;

/**
 * カットイン中に使用するカメラワークの意味的な種類。
 *
 * <p>能力実装は具体的なカメラ座標ではなく演出意図を指定し、描画側が画面構成に
 * 合わせた軌道へ変換する。</p>
 *
 * <h2>関係</h2>
 * <p>{@link CutInSpec}が一つ保持し、ホストのカメラ制御とHUD描画から参照される。
 * Abilityはカメラ実装へ直接依存しない。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * CutInSpec spec = new CutInSpec(image, titleKey, 1.2f, 100,
 *         CutInMotion.SLASH_RUSH, 1f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>画面サイズやカメラ座標を公開APIへ固定せず、演出の意図だけを安定した列挙値として共有するために作成された。</p>
 */
public enum CutInMotion {
    /**
     * 発動者へ急接近した後、斜めに回り込んで攻撃方向へ抜ける。
     */
    SLASH_RUSH,
    /**
     * 低い位置から横方向へ高速移動し、衝突直前に大きく引く。
     */
    HORIZONTAL_CHARGE,
    /**
     * 発動者を中心に回り込みながら寄り、最後に戦場全体を映す。
     */
    ORBIT_RUSH
}
