package com.takoyaki3466.mojibattle.tree;

import jp.takoa.mojibattle.ability.config.*;
import jp.takoa.mojibattle.ability.context.AbilityActivationContext;
import jp.takoa.mojibattle.ability.runtime.SkillRuntime;
import jp.takoa.mojibattle.battle.damage.DamageType;
import jp.takoa.mojibattle.resource.ResourceId;

public final class ReturnToForestSkill extends AbstractRuntime<ReturnToForestSkill.Config> implements SkillRuntime {

    public ReturnToForestSkill(Config config) {
        super(config);
    }

    @Override
    public boolean tryActivate(AbilityActivationContext context) {
        var target = context.target();
        if (target == null || !target.alive()) {
            return false;
        }

        context.actions().damage(context.user(), target, getConfig().damage(), DamageType.SKILL);
        context.actions().knockbackAwayFrom(context.user(), target, getConfig().knockbackDistance());
        context.actions().emitEffect(target, getConfig().impactEffect(), getConfig().effectScale(), getConfig().effectDurationSeconds());

        return true;
    }


    public record Config(
            @ConfigFloat(defaultValue = 30f) @DisplayValue float damage,
            @ConfigFloat(defaultValue = 1.4f) float knockbackDistance,
            @ConfigResource(name = "impactEffectId", defaultValue = "mojibattle:wind")
            ResourceId impactEffect,
            @ConfigFloat(defaultValue = 4.5f, exclusiveMin = true) float effectScale,
            @ConfigFloat(defaultValue = 0.45f, exclusiveMin = true) float effectDurationSeconds
    ) {
    }

}
