package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.resource.ResourceId;

/**
 * {@link ProjectilePatternSpec}を構成する一発分の表示、相対位置、命中効果。
 *
 * <h2>関係</h2>
 * <p>任意の画像を{@link ResourceId}で参照し、{@link ProjectilePatternSpec}だけが集合として保持する。
 * ホストの編隊生成処理が個別の投射物へ変換する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * ProjectileShotSpec left = new ProjectileShotSpec("左", texture,
 *         -0.8f, 0f, 12f, 0.5f, 0f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>編隊全体の速度・寿命と、各弾固有の見た目・配置・行動不能効果を分けて記述するために作成された。</p>
 *
 * @param glyph          画像がない場合にも表示できる代替文字
 * @param texture        任意の弾画像ID
 * @param lateralOffset  編隊中心からの横方向オフセット
 * @param verticalOffset 編隊中心からの縦方向オフセット
 * @param baseDamage     個別の基礎ダメージ
 * @param stunSeconds    命中時のスタン秒
 * @param bindSeconds    命中時の移動拘束秒
 */
public record ProjectileShotSpec(String glyph, ResourceId texture, float lateralOffset, float verticalOffset,
                                 float baseDamage, float stunSeconds, float bindSeconds) {
    public ProjectileShotSpec {
        if (glyph == null || !finite(lateralOffset) || !finite(verticalOffset) || !nonNegative(baseDamage) || !nonNegative(stunSeconds) || !nonNegative(bindSeconds)) {
            throw new IllegalArgumentException("Projectile shot values are invalid");
        }
    }

    private static boolean finite(float value) {
        return Float.isFinite(value);
    }

    private static boolean nonNegative(float value) {
        return finite(value) && value >= 0;
    }
}
