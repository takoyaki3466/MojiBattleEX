package jp.takoa.mojibattle.lwjgl3;

/** プラットフォーム固有のJVM再起動方針を隔離する。Windows版では再起動を必要としない。 */
final class StartupHelper {
    private StartupHelper() {
    }

    static boolean startNewJvmIfRequired() {
        return false;
    }
}
