package jp.takoa.mojibattle.domain.ability;

public final class ConfiguredUltimateFactory {
    private ConfiguredUltimateFactory(){}
    public static IUltimate create(AbilityConfigs.AreaUltimate config){
        return switch(config.type){
            case "self_heal" -> new SelfHealUltimate(config);
            case "line" -> new LineUltimate(config);
            case "meteor_shower" -> new MeteorShowerUltimate(config);
            case "summon_family" -> new FamilyUltimate(config);
            case "moving_car" -> new MovingCarUltimate(config);
            case "spinning_one" -> new SpinningOneUltimate(config);
            case "skill_barrage" -> new SkillBarrageUltimate(config);
            case "area" -> new BankruptcyUltimate(config);
            default -> throw new IllegalArgumentException("Unknown ultimate type: "+config.type);
        };
    }
}
