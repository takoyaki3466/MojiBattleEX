package jp.takoa.mojibattle.input;

import com.badlogic.gdx.math.Vector2;
import jp.takoa.mojibattle.domain.PlayerId;

/**
 * コントローラー設定画面へ公開する、入力を消費しないJoy-Conの現在状態。
 */
public record JoyConLiveState(
        PlayerId player, String deviceId, String name, JoyConSide side, boolean connected,
        Vector2 movement, boolean mainSkill, boolean mainUltimate, boolean supportSkill,
        boolean supportUltimate) {
}
