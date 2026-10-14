package jp.takoa.mojibattle.domain.ability;

import jp.takoa.mojibattle.registry.Registry;

/**
 * スキル、必殺技、パッシブを種別ごとのIDで登録・検索するレジストリ。
 */
public final class AbilityRegistry {
    private final Registry<ISkill> skills = new Registry<>("skill", ISkill::id);
    private final Registry<IUltimate> ultimates = new Registry<>("ultimate", IUltimate::id);
    private final Registry<IPassive> passives = new Registry<>("passive", IPassive::id);

    public void register(ISkill value) {
        skills.register(value);
    }

    public void register(IUltimate value) {
        ultimates.register(value);
    }

    public void register(IPassive value) {
        passives.register(value);
    }

    public ISkill skill(String id) {
        return skills.require(id);
    }

    public IUltimate ultimate(String id) {
        return ultimates.require(id);
    }

    public IPassive passive(String id) {
        return passives.require(id);
    }

    public void freeze() {
        skills.freeze();
        ultimates.freeze();
        passives.freeze();
    }
}
