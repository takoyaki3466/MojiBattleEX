package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.battle.CombatantPosition;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.BattleController;

/** CharacterRuntimeを公開読取り契約だけへ射影する。 */
public final class CoreCombatantView implements CombatantView {
    private final CharacterRuntime runtime;
    private final BattleController battle;

    public CoreCombatantView(CharacterRuntime runtime) {
        this(null, runtime);
    }

    public CoreCombatantView(BattleController battle, CharacterRuntime runtime) {
        this.battle = battle;
        this.runtime = runtime;
    }

    CharacterRuntime runtime() {
        return runtime;
    }

    @Override
    public String instanceId() {
        return battle == null ? runtime.playerId.name().toLowerCase() + ":main:" + runtime.definition.id
                + ":" + Integer.toUnsignedString(System.identityHashCode(runtime), 36)
                : battle.instanceIdOf(runtime);
    }

    @Override
    public String id() {
        return runtime.definition.id;
    }

    @Override
    public float hp() {
        return runtime.hp();
    }

    @Override
    public float maximumHp() {
        return runtime.maxHp();
    }

    @Override
    public float ultimatePoints() {
        return runtime.ultimatePoints();
    }

    @Override
    public boolean alive() {
        return runtime.isAlive();
    }

    @Override public CombatantPosition position() {
        return new CombatantPosition(runtime.position.x, runtime.position.y, runtime.position.z);
    }

    @Override public float defenseMultiplier() {
        return battle == null ? 1f : battle.combatantDefenseMultiplier(runtime);
    }
}
