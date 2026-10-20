package jp.takoa.mojibattle.battle.combatant;

/**
 * 描画・物理ライブラリに依存しない、戦場内の三次元座標値。
 *
 * <h2>関係</h2>
 * <p>{@link CombatantView#position()}が返し、固定座標へ落下攻撃を生成する
 * {@link jp.takoa.mojibattle.battle.action.spec.FallingStrikeSpec}などが保持する。
 * ホストが内部ベクトルとの相互変換を担当する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * CombatantPosition impact = target.position();
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>libGDX等の型を公開APIへ漏らさず、生成時点の位置を安全な不変値として保存するために作成された。</p>
 *
 * @param x 戦場の横軸座標
 * @param y 高さ座標
 * @param z 戦場の奥行き座標
 */
public record CombatantPosition(float x, float y, float z) {
    public static final CombatantPosition ORIGIN = new CombatantPosition(0, 0, 0);

    public CombatantPosition {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Combatant position must be finite");
        }
    }
}
