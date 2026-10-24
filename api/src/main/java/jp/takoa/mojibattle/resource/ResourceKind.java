package jp.takoa.mojibattle.resource;

/**
 * JAR内で名前空間化されるリソースルート。
 */
public enum ResourceKind {
    ASSET("assets"),
    DATA("data");

    private final String directory;

    ResourceKind(String directory) {
        this.directory = directory;
    }

    public String directory() {
        return directory;
    }
}
