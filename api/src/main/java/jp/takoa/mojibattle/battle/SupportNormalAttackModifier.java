package jp.takoa.mojibattle.battle;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 補助通常攻撃の補正。MULTIPLYは数値を乗算、ADDは数値を加算、REPLACEは値を置換する。
 * REPLACE同士はpriority最大、同値ならID辞書順が後のものを採用する。画像と演出も同じ規則で選ぶ。
 * projectileCount/hitsPerAttackは合成後に1以上へクランプされる。
 * ADDの中立値は数値0、MULTIPLYの中立値は数値1。REPLACEは全数値を指定値へ置換する。
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
    public enum Stacking { ADD, MULTIPLY, REPLACE }
}
