package jp.takoa.mojibattle.domain.ability;

/** 設定の必殺技種別を対応する実装へ変換する生成ファクトリ。 */
public final class ConfiguredUltimateFactory {
    private ConfiguredUltimateFactory() {
    }

    /**
     * 設定の {@code type} に対応する必殺技を生成する。
     *
     * @throws IllegalArgumentException 未対応の種別が指定された場合
     */
    public static IUltimate create(AbilityConfigs.AreaUltimate config) {
        return switch (config.type) {
            case "self_heal" -> new SelfHealUltimate(config);
            case "line" -> new LineUltimate(config);
            case "meteor_shower" -> new MeteorShowerUltimate(config);
            case "summon_family" -> new FamilyUltimate(config);
            case "moving_car" -> new MovingCarUltimate(config);
            case "spinning_one" -> new SpinningOneUltimate(config);
            case "skill_barrage" -> new SkillBarrageUltimate(config);
            case "sushi_barrage" -> new SushiBombingUltimate(config);
            case "area" -> new BankruptcyUltimate(config);
            default -> throw new IllegalArgumentException("Unknown ultimate type: " + config.type);
        };
    }
}
