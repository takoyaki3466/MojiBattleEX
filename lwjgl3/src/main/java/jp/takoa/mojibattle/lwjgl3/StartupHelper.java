package jp.takoa.mojibattle.lwjgl3;

/** Placeholder for platform-specific JVM restart policy. Windows requires no restart. */
final class StartupHelper {
    private StartupHelper() {}
    static boolean startNewJvmIfRequired() { return false; }
}
