package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.SkillRuntime;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.domain.ability.ISkill;

/** 公開SkillRuntimeを既存の戦闘Ability契約へ接続する。 */
public final class ExternalSkillAdapter implements ISkill {
    private final AbilityConfigs.ProjectileSkill config;
    private final SkillRuntime runtime;

    public ExternalSkillAdapter(AbilityConfigs.ProjectileSkill config, SkillRuntime runtime) {
        this.config = config;
        this.runtime = runtime;
    }

    @Override
    public String id() {
        return config.id;
    }

    @Override
    public boolean tryActivate(BattleController battle, CharacterRuntime user) {
        if (!battle.acceptsActions() || !user.isAlive() || user.isStunned() || !user.skillReady()) {
            return false;
        }
        if (!runtime.tryActivate(ExternalAbilityContexts.create(battle, user))) {
            return false;
        }
        user.startSkillCooldown(battle.skillCooldown(user, config.cooldownSeconds));
        return true;
    }

    @Override
    public void activateFromUltimate(BattleController battle, CharacterRuntime user) {
        runtime.activateFromUltimate(ExternalAbilityContexts.create(battle, user));
    }
}
