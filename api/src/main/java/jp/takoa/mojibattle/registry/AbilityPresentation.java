package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UIで表示する、翻訳キーと引数からなるAbility説明。
 */
public record AbilityPresentation(List<Line> lines) {
    private static final String DESCRIPTION_KEY = "descriptionKey";

    public AbilityPresentation {
        lines = List.copyOf(lines);
    }

    public static AbilityPresentation of(String key, Object... arguments) {
        return new AbilityPresentation(List.of(new Line(key, arguments)));
    }

    /**
     * Ability JSONの{@code descriptionKey}を使用する既定Presentationを生成する。
     * 実装登録IDから翻訳キーを推測せず、データが宣言した所有キャラクター固有のキーを維持する。
     */
    public static AbilityPresentation fromDescription(ConfigView config, Object... arguments) {
        if (config == null) {
            throw new IllegalArgumentException("Ability config is required for presentation");
        }
        String key = config.stringValue(DESCRIPTION_KEY, null);
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Ability presentation requires a non-blank JSON descriptionKey");
        }
        return of(key, arguments);
    }

    public AbilityPresentation append(String key, Object... arguments) {
        ArrayList<Line> next = new ArrayList<>(lines);
        next.add(new Line(key, arguments));
        return new AbilityPresentation(next);
    }

    public record Line(String key, Object[] arguments) {
        public Line {
            arguments = Arrays.copyOf(arguments, arguments.length);
        }
    }
}
