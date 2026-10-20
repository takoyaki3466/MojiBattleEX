package jp.takoa.mojibattle.battle.action.spec;

/**
 * 登録済みの別スキルを、元スキルの資源を消費せず一定間隔で反復発動する仕様。
 *
 * <h2>関係</h2>
 * <p>スキル登録IDを文字列で参照し、{@link jp.takoa.mojibattle.battle.action.CombatActions#startSkillBarrage}へ渡す。
 * ホストのAbilityレジストリと発動スケジューラーから参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.startSkillBarrage(user, new SkillBarrageSpec("example:slash", 5, 0.2f));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>登録済みスキルの挙動を再利用しつつ、クールダウンや通常コストの二重消費を避ける連続技をホスト管理で実現するために作成された。</p>
 *
 * @param skillId 反復発動する登録済みスキルID
 * @param count 発動回数
 * @param intervalSeconds 発動間隔秒
 */
public record SkillBarrageSpec(String skillId, int count, float intervalSeconds) {
    public SkillBarrageSpec {
        if (skillId == null || skillId.isBlank() || count < 1 || count > 256
                || !Float.isFinite(intervalSeconds) || intervalSeconds <= 0) {
            throw new IllegalArgumentException("Skill barrage values are invalid");
        }
    }
}
