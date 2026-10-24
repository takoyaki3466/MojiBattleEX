package jp.takoa.mojibattle.registry;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code namespace:path} 形式でRegistry要素を識別する不変ID。
 */
public record RegistryId(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "mojibattle";
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public RegistryId {
        namespace = normalize(namespace, "namespace");
        path = normalize(path, "path");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid registry ID: " + namespace + ":" + path);
        }
    }

    /**
     * 名前空間なしの旧IDを {@value #DEFAULT_NAMESPACE} 名前空間として解釈する。
     */
    public static RegistryId parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Registry ID is required");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');

        if (separator < 0) {
            return new RegistryId(DEFAULT_NAMESPACE, normalized);
        }

        if (separator != normalized.lastIndexOf(':')) {
            throw new IllegalArgumentException("Invalid registry ID: " + value);
        }

        return new RegistryId(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    public static RegistryId of(String namespace, String path) {
        return new RegistryId(namespace, path);
    }

    private static String normalize(String value, String part) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Registry ID " + part + " is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
