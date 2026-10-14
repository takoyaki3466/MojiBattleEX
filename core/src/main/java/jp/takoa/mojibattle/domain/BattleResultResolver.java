package jp.takoa.mojibattle.domain;

import java.util.Collection;
import java.util.List;

/**
 * 生存状況と残HP率から、対戦の終了可否および勝敗を判定する。
 */
public final class BattleResultResolver {
    private static final float RATIO_EPSILON = 0.00001f;

    /**
     * 現在の試合状態から結果を算出する。
     *
     * @param players     判定対象の全プレイヤー
     * @param timeExpired 制限時間が終了しているか
     * @return 確定した試合結果。試合継続中の場合は {@code null}
     */
    public BattleResult resolve(Collection<CharacterRuntime> players, boolean timeExpired) {
        List<CharacterRuntime> alive = players.stream().filter(CharacterRuntime::isAlive).toList();

        if (alive.size() == 1) {
            return new BattleResult(alive.get(0).playerId, BattleResult.Reason.KNOCKOUT);
        }

        if (alive.isEmpty()) {
            return new BattleResult(null, BattleResult.Reason.DRAW);
        }

        if (!timeExpired) {
            return null;
        }

        float best = alive.stream().map(CharacterRuntime::hpRatio).max(Float::compare).orElse(0f);
        List<CharacterRuntime> leaders = alive.stream().filter(player -> Math.abs(player.hpRatio() - best) <= RATIO_EPSILON).toList();

        return leaders.size() == 1 ? new BattleResult(leaders.get(0).playerId, BattleResult.Reason.TIMEOUT) : new BattleResult(null, BattleResult.Reason.DRAW);
    }
}
