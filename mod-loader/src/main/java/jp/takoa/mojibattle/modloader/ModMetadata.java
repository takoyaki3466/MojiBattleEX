package jp.takoa.mojibattle.modloader;

import java.util.HashMap;

/**
 * {@code META-INF/mojibattle/mod.json} の静的メタデータ。
 */
public final class ModMetadata {
    public int schemaVersion;
    public String id;
    public String name;
    public String version;
    public String apiVersion;
    public String gameVersion;
    public String entrypoint;
    public Dependencies dependencies = new Dependencies();

    /**
     * requiredとoptionalの依存MOD ID、最低versionを保持する。
     */
    public static class Dependencies {
        public HashMap<String, String> required = new HashMap<>();
        public HashMap<String, String> optional = new HashMap<>();
    }
}
