package jp.takoa.mojibattle.item.init;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.registry.AbilityPresentation;

/** 組み込み能力のJSON由来Presentationを共有する。 */
final class AbilityRegistrationSupport {
    private AbilityRegistrationSupport() {
    }

    static AbilityPresentation presentation(ConfigView config) {
        return AbilityPresentation.fromDescription(config);
    }
}
