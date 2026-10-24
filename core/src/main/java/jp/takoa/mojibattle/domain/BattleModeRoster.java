package jp.takoa.mojibattle.domain;

/**
 * 対戦モードごとの参加人数とチーム編成数を定義する。
 */
public final class BattleModeRoster {
    public static final int MEGA_MAIN_COUNT = 20;
    public static final int MEGA_SUPPORT_COUNT = 30;

    private BattleModeRoster() {
    }

    /**
     * 操作プレイヤー一人が選択するメインキャラクター数。
     */
    public static int mainCount(BattleMode mode) {
        return switch (mode) {
            case CHAOS -> 3;
            case MEGA_GALAXY -> MEGA_MAIN_COUNT;
            default -> 1;
        };
    }

    /**
     * 操作プレイヤー一人が選択する補助キャラクター数。
     */
    public static int supportCount(BattleMode mode) {
        return switch (mode) {
            case CHAOS -> 4;
            case MEGA_GALAXY -> MEGA_SUPPORT_COUNT;
            default -> 1;
        };
    }

    /**
     * 通常のカオスモードと同様の代表・追従メイン編成を使用するか。
     */
    public static boolean chaosFormation(BattleMode mode) {
        return mode == BattleMode.CHAOS || mode == BattleMode.MEGA_GALAXY;
    }
}
