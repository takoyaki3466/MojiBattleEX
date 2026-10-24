package jp.takoa.mojibattle.domain.ability.adapter;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.battle.combatant.CombatantKind;
import jp.takoa.mojibattle.battle.combatant.CombatantPosition;
import jp.takoa.mojibattle.battle.combatant.CombatantView;
import jp.takoa.mojibattle.domain.BattleController;

/**
 * BattleControllerが所有する任意種別の個体を公開CombatantViewへ射影する。
 */
public final class CoreUnifiedCombatantView implements CombatantView {
    private final BattleController battle;
    private final Object combatant;

    public CoreUnifiedCombatantView(BattleController battle, Object combatant) {
        this.battle = battle;
        this.combatant = combatant;
        if (!battle.containsBattleCombatant(combatant)) throw new IllegalArgumentException("Unknown combatant");
    }

    BattleController battle() {
        return battle;
    }

    Object combatant() {
        return combatant;
    }

    @Override
    public String id() {
        return battle.definitionIdOf(combatant);
    }

    @Override
    public String instanceId() {
        return battle.instanceIdOf(combatant);
    }

    @Override
    public CombatantKind kind() {
        return battle.kindOf(combatant);
    }

    @Override
    public float hp() {
        return battle.hpOf(combatant);
    }

    @Override
    public float maximumHp() {
        return battle.maximumHpOf(combatant);
    }

    @Override
    public float ultimatePoints() {
        return battle.ultimatePointsOf(combatant);
    }

    @Override
    public boolean alive() {
        return battle.alive(combatant);
    }

    @Override
    public float defenseMultiplier() {
        return battle.combatantDefenseMultiplier(combatant);
    }

    @Override
    public CombatantPosition position() {
        Vector3 value = battle.positionOf(combatant);
        return new CombatantPosition(value.x, value.y, value.z);
    }
}
