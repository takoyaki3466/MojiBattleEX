package jp.takoa.mojibattle.battle.modifier;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 補助通常攻撃の補正。MULTIPLYは数値を乗算、ADDは数値を加算、REPLACEは値を置換する。
 * REPLACE同士はpriority最大、同値ならID辞書順が後のものを採用する。画像と演出も同じ規則で選ぶ。
 * projectileCount/hitsPerAttackは合成後に1以上へクランプされる。
 * ADDの中立値は数値0、MULTIPLYの中立値は数値1。REPLACEは全数値を指定値へ置換する。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.support.SupportActions#addNormalAttackModifier}へ渡され、
 * ホストの補助通常攻撃合成処理から参照される。合成方式は{@link Stacking}、識別子と表示資源は{@link ResourceId}で表す。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * SupportNormalAttackModifier boost = new SupportNormalAttackModifier(id, 20,
 *         SupportNormalAttackModifier.Stacking.MULTIPLY,
 *         1.5f, 0.8f, 1, 1, 1f, texture, effect);
 * context.supportActions().addNormalAttackModifier(boost, 5f);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>補助能力では加算・乗算・置換の意図が異なるため、メイン用のdelta型補正と分離して明示的に合成できるようにした。</p>
 *
 * @param id 補正の安定ID
 * @param priority REPLACE値と画像・演出の選択優先度
 * @param stacking 数値の合成方式
 * @param damageMultiplier ダメージの合成値
 * @param intervalMultiplier 攻撃間隔の合成値
 * @param projectileCount 投射物数の合成値
 * @param hitsPerAttack 一回の攻撃あたりhit数の合成値
 * @param spreadAngle 弾列全体の扇角度
 * @param projectileTexture 任意の弾画像ID
 * @param effect 任意の発射・命中演出ID
 */
public record SupportNormalAttackModifier(ResourceId id, int priority, Stacking stacking,
        float damageMultiplier, float intervalMultiplier, int projectileCount, int hitsPerAttack,
        float spreadAngle, ResourceId projectileTexture, ResourceId effect) {
    public SupportNormalAttackModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stacking, "stacking");
        if (!Float.isFinite(damageMultiplier) || !Float.isFinite(intervalMultiplier)
                || !Float.isFinite(spreadAngle) || intervalMultiplier < 0 || spreadAngle < 0) {
            throw new IllegalArgumentException("Invalid support normal attack modifier");
        }
    }
    /** 補助通常攻撃補正を既存値へ合成する方式。 */
    public enum Stacking { ADD, MULTIPLY, REPLACE }
}
