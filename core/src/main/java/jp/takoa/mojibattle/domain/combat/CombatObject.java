package jp.takoa.mojibattle.domain.combat;

import com.badlogic.gdx.math.Vector3;

/**
 * 戦場上で時間経過により更新され、位置と終了状態を公開する一時オブジェクトの共通契約。
 */
public sealed interface CombatObject permits PositionedCombatObject, SushiBarrage {
    /** @param delta 前回更新からの経過秒数 */
    void update(float delta);

    /** @return オブジェクトを戦場から除去すべき場合は {@code true} */
    boolean expired();

    /** @return 現在のワールド座標 */
    Vector3 position();
}
