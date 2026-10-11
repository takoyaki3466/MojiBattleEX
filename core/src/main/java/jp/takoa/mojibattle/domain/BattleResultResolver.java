package jp.takoa.mojibattle.domain;
import java.util.*;

public final class BattleResultResolver {
    private static final float RATIO_EPSILON = 0.00001f;

    public BattleResult resolve(Collection<CharacterRuntime> players,boolean timeExpired) {
        List<CharacterRuntime> alive=players.stream().filter(CharacterRuntime::isAlive).toList();
        if(alive.size()==1)return new BattleResult(alive.get(0).playerId,BattleResult.Reason.KNOCKOUT);
        if(alive.isEmpty())return new BattleResult(null,BattleResult.Reason.DRAW);
        if(!timeExpired)return null;
        float best=alive.stream().map(CharacterRuntime::hpRatio).max(Float::compare).orElse(0f);
        List<CharacterRuntime> leaders=alive.stream().filter(p->Math.abs(p.hpRatio()-best)<=RATIO_EPSILON).toList();
        return leaders.size()==1?new BattleResult(leaders.get(0).playerId,BattleResult.Reason.TIMEOUT):new BattleResult(null,BattleResult.Reason.DRAW);
    }
}
