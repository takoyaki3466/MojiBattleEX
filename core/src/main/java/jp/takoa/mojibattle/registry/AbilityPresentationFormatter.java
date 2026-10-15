package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.i18n.LocalizationManager;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 能力の表示メタデータを、現在の言語に対応した複数行テキストへ変換する。
 */
public final class AbilityPresentationFormatter {
    private AbilityPresentationFormatter() {
    }

    public static String format(AbilityPresentation presentation, LocalizationManager i18n) {
        return presentation.lines().stream().map(line -> format(line, i18n)).collect(Collectors.joining("\n"));
    }

    private static String format(AbilityPresentation.Line line, LocalizationManager i18n) {
        Object[] arguments = Arrays.stream(line.arguments()).map(argument -> argument instanceof AbilityPresentation.Line nested ? format(nested, i18n) : argument).toArray();

        return i18n.text(line.key(), arguments);
    }
}
