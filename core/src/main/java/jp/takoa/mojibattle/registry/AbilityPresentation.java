package jp.takoa.mojibattle.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UIが能力種別を判定せず表示できる、翻訳キーと引数からなる説明メタデータ。
 */
public record AbilityPresentation(List<Line> lines) {
    public AbilityPresentation {
        lines = List.copyOf(lines);
    }

    public static AbilityPresentation of(String key, Object... arguments) {
        return new AbilityPresentation(List.of(new Line(key, arguments)));
    }

    public AbilityPresentation append(String key, Object... arguments) {
        ArrayList<Line> next = new java.util.ArrayList<>(lines);
        next.add(new Line(key, arguments));

        return new AbilityPresentation(next);
    }

    /**
     * 1行分の翻訳キーと置換引数。
     */
    public record Line(String key, Object[] arguments) {
        public Line {
            arguments = Arrays.copyOf(arguments, arguments.length);
        }
    }
}
