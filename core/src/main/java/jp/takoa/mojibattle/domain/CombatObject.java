package jp.takoa.mojibattle.domain;

import com.badlogic.gdx.math.Vector3;

public sealed interface CombatObject permits Projectile, UltimateStrike, VisualEffect, LineStrike, FamilySummon, CarHazard, SpinningOne, AwakeningItem, SkillBarrage {
    void update(float delta);
    boolean expired();
    Vector3 position();
}
