package jp.takoa.mojibattle.runtime.factory;

import jp.takoa.mojibattle.config.ConfigManager;
import jp.takoa.mojibattle.data.RegistryResolver;
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

        data.skills().forEach(definition -> abilities.registerSkill(
                definition.id,
                () -> new AbilityRegistry.ConfiguredSkill(definition.id, definition.cooldownSeconds,
                        definition.maximumStocks, definition.initialStocks, definition.stockRechargeSeconds,
                        definition.healthCostRatio,
                        resolver.skill(definition.implementationId).factory().create(MapConfigView.of(definition)))));
        data.ultimates().forEach(definition -> abilities.registerUltimate(
                definition.id,
                () -> new AbilityRegistry.ConfiguredUltimate(definition.id, definition.nameKey, definition.pointCost,
                        resolver.ultimate(definition.implementationId).factory().create(MapConfigView.of(definition)))));
        data.passives().forEach(definition -> abilities.registerPassive(
                definition.id,
                () -> resolver.passive(definition.implementationId).factory().create(MapConfigView.of(definition))));

        abilities.freeze();
        return abilities;
    }
}
