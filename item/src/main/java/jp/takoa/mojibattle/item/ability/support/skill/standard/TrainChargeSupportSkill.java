package jp.takoa.mojibattle.item.ability.support.skill.standard;

import jp.takoa.mojibattle.ability.config.AbstractRuntime;
import jp.takoa.mojibattle.ability.config.annotations.ConfigFloat;
import jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime;
import jp.takoa.mojibattle.battle.action.spec.MovingHazardSpec;
import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

/** 前進する列車ハザードを生成する補助スキル。 */
public final class TrainChargeSupportSkill
        extends AbstractRuntime<TrainChargeSupportSkill.Config> implements SupportAbilityRuntime {
    public TrainChargeSupportSkill(Config config) {
        super(config);
    }

    @Override
    public boolean activate(SupportAbilityActivationContext context) {
        Config config = getConfig();
        context.actions().spawnMovingHazards(context.owner(), new MovingHazardSpec(
                1, config.speed(), 2.1f, DamageRequest.of(config.damage(), DamageType.ULTIMATE),
                config.stunSeconds(), config.duration(),
                new ResourceId("mojibattle", "objects/commuter-train.png"),
                new ResourceId("mojibattle", "vehicle")));
        return true;
    }

    /** JSONから読み取る列車突進設定。 */
    public record Config(
            @ConfigFloat(name = "range", defaultValue = 1f, exclusiveMin = true) float speed,
            @ConfigFloat(name = "baseDamage", defaultValue = 0f) float damage,
            @ConfigFloat(name = "magnitude", defaultValue = 0f) float stunSeconds,
            @ConfigFloat(name = "durationSeconds", defaultValue = 1f, exclusiveMin = true) float duration) {
    }
}
