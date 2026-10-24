package jp.takoa.mojibattle.modloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 初期MOD APIが必要とするmajor/minor/patchだけを扱う厳格なversion値。
 */
public record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
    private static final Pattern FORMAT = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)");

    public static SemanticVersion parse(String value) {
        Matcher matcher = FORMAT.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + value);
        }
        return new SemanticVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int majorResult = Integer.compare(major, other.major);
        if (majorResult != 0) {
            return majorResult;
        }
        int minorResult = Integer.compare(minor, other.minor);
        return minorResult != 0 ? minorResult : Integer.compare(patch, other.patch);
    }
}
