package jp.takoa.mojibattle.runtime.factory;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.data.RegistryResolver;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.AbilityRegistry;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;

/**
 * 検証済みDefinitionと登録済みType Factoryから、プレイヤー専用インスタンスを生成する
 * Supplierカタログを構築する。
 */
public final class AbilityRuntimeFactory {
    private final RegistryResolver resolver;

    public AbilityRuntimeFactory(RegistryResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 全Ability定義を実装IDで解決し、状態を共有しない実行インスタンスへ変換する。
     * 生成後のレジストリは凍結されるため、呼び出し側は追加登録できない。
     *
     * @param data 検証と参照解決を終えた設定データ
     * @return スキル・必殺技・パッシブの実行レジストリ
     */
    public AbilityRegistry create(ConfigManager data) {
        AbilityRegistry abilities = new AbilityRegistry();

        skill(data, abilities);
        ultimate(data, abilities);
        passive(data, abilities);

        abilities.freeze();
        return abilities;
    }

    private void skill(ConfigManager data, AbilityRegistry abilities) {
        for (AbilityConfigs.ProjectileSkill projectileSkill : data.skills()) {
            abilities.registerSkill(projectileSkill.id, () -> new AbilityRegistry.ConfiguredSkill(projectileSkill.id, projectileSkill.cooldownSeconds, projectileSkill.maximumStocks, projectileSkill.initialStocks, projectileSkill.stockRechargeSeconds, projectileSkill.healthCostRatio, resolver.skill(projectileSkill.implementationId).factory().create(MapConfigView.of(projectileSkill))));
        }
    }

    private void ultimate(ConfigManager data, AbilityRegistry abilities) {
        for (AbilityConfigs.AreaUltimate areaUltimate : data.ultimates()) {
            abilities.registerUltimate(areaUltimate.id, () -> new AbilityRegistry.ConfiguredUltimate(areaUltimate.id, areaUltimate.nameKey, areaUltimate.pointCost, resolver.ultimate(areaUltimate.implementationId).factory().create(MapConfigView.of(areaUltimate))));
        }
    }

    private void passive(ConfigManager data, AbilityRegistry abilities) {
        for (AbilityConfigs.TimedPassive definition : data.passives()) {
            abilities.registerPassive(definition.id, () -> resolver.passive(definition.implementationId).factory().create(MapConfigView.of(definition)));
        }
    }
}
