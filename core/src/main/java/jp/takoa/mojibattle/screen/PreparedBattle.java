package jp.takoa.mojibattle.screen;

import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.input.PlayerInputManager;
import jp.takoa.mojibattle.render.SplitScreenRenderer;
import jp.takoa.mojibattle.ui.BattleHud;

/**
 * 戦闘画面の生成に必要な、初期化済みの戦闘要素一式。
 *
 * <p>{@link BattleScreen} へ渡すまでは生成した側が所有し、受け渡しに失敗した場合は
 * {@link #disposeAfterFailure(Throwable)} で解放する。要素はnullを許容する（途中まで構築した場合）。</p>
 */
record PreparedBattle(BattleController battle, PlayerInputManager input, SplitScreenRenderer renderer, BattleHud hud) {
    /**
     * 構築済みの要素を後から作ったものから順に解放し、解放時の失敗は元の例外へsuppressedとして付与する。
     */
    void disposeAfterFailure(Throwable failure) {
        if (hud != null) {
            BattleScreen.closeAfterFailure(failure, hud::dispose);
        }
        if (renderer != null) {
            BattleScreen.closeAfterFailure(failure, renderer::dispose);
        }
        if (input != null) {
            BattleScreen.closeAfterFailure(failure, input::clear);
        }
        if (battle != null) {
            BattleScreen.closeAfterFailure(failure, battle::close);
        }
    }
}
