package jp.takoa.mojibattle.ability.support;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 補助の一時分裂で生成する、召喚個体一体分の外観と通常攻撃能力。
 *
 * <h2>関係</h2>
 * <p>画像は{@link ResourceId}で参照し、複数の本型を{@link SupportSplitSpec}が保持する。
 * {@link SupportActions#splitTemporarily(SupportSplitSpec)}を実装するホストが召喚データへ変換する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SupportSplitMemberSpec member = new SupportSplitMemberSpec(
 *         "分", ResourceId.parse("example:textures/split.png"), 8f, 0.7f, 4f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>分裂後の個体ごとに異なる見た目と能力値を指定しながら、coreの召喚実装をAPIへ公開しないために作成された。</p>
 *
 * @param displayGlyph          画像が使用できない場合にも表示できる代替文字
 * @param texture               召喚個体の画像ID
 * @param attackDamage          通常攻撃一回の基礎ダメージ
 * @param attackIntervalSeconds 通常攻撃間隔秒
 * @param moveSpeed             秒あたりの移動速度
 */
public record SupportSplitMemberSpec(String displayGlyph, ResourceId texture, float attackDamage, float attackIntervalSeconds, float moveSpeed) {
    public SupportSplitMemberSpec {
        Objects.requireNonNull(displayGlyph, "displayGlyph");
        Objects.requireNonNull(texture, "texture");

        if (!Float.isFinite(attackDamage) || attackDamage < 0 || !Float.isFinite(attackIntervalSeconds) || attackIntervalSeconds <= 0 || !Float.isFinite(moveSpeed) || moveSpeed < 0) {
            throw new IllegalArgumentException("Invalid split member statistics");
        }
    }
}
