package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.battle.CombatantView;
import jp.takoa.mojibattle.domain.CharacterRuntime;

/** CharacterRuntimeを公開読取り契約だけへ射影する。 */
public final class CoreCombatantView implements CombatantView {
    private final CharacterRuntime runtime;

    public CoreCombatantView(CharacterRuntime runtime) {
        this.runtime = runtime;
    }

    CharacterRuntime runtime() {
        return runtime;
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
}
