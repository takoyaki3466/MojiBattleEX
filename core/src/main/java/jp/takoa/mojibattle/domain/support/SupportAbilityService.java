package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.audio.BattleSoundCue;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.domain.ability.adapter.ExternalSupportAbilityContexts;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;

/**
 * 登録済み補助能力を解決し、発動条件・消費・共通演出だけを制御する。
 */
public final class SupportAbilityService {
    private static final String[] SKILL_EFFECTS = {EffectIds.WIND, EffectIds.KANA};
    private static final String[] ULTIMATE_EFFECTS = {EffectIds.STAR, EffectIds.FAMILY, EffectIds.NEUTRAL, EffectIds.STOCK};
    private final Registry<SupportAbilityImpl> implementations;

    public SupportAbilityService(Registry<SupportAbilityImpl> implementations) {
        this.implementations = implementations;
    }

    /**
     * 補助スキルが使用可能でメインが行動可能なら、クールタイムを先に確定して実装を呼び出す。
     */
    public boolean useSkill(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        SupportAbilityDefinition ability = support.definition.skill;

        if (!support.skillReady() || team.main().isStunned()) {
            return false;
        }

        if (execute(battle, team, support, ability, false)) {
            support.startSkillCooldown(battle.supportSkillCooldown(ability.cooldownSeconds));
            return true;
        }
        return false;
    }

    /**
     * メインが行動可能かつ補助専用ゲージを消費できる場合だけ必殺を実行し、共通告知を出す。
     */
    public boolean useUltimate(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        SupportAbilityDefinition ability = support.definition.ultimate;

        if (team.main().isStunned() || support.ultimatePoints() < ability.pointCost) {
            return false;
        }

        long previousCutInRevision = battle.cutInRevision();
        if (execute(battle, team, support, ability, true)) {
            support.spendUltimate(ability.pointCost);
            if (battle.cutInRevision() == previousCutInRevision) {
                battle.showDefaultUltimateCutIn(team.main(), support.definition.glyphTexture, ability.nameKey);
            }
            battle.announceUltimate(team.main().playerId, ability.nameKey);
            return true;
        }
        return false;
    }

    /**
     * 実装IDを解決して固有処理を呼び、補助能力共通の効果音と多重リング演出を重ねる。
     */
    private boolean execute(BattleController battle, PlayerTeam team, SupportCharacterRuntime support,
                            SupportAbilityDefinition ability, boolean ultimate) {
        SupportAbilityImpl implementation = implementations.require(ability.type);
        var runtime = implementation.factory().create(MapConfigView.of(ability));
        if (runtime == null || !runtime.activate(ExternalSupportAbilityContexts.create(battle, team, support, ability))) {
            return false;
        }
        battle.emitSound(BattleSoundCue.SUPPORT);

        for (int ring = 0; ring < (ultimate ? 4 : 2); ring++) {
            battle.emitEffect(support.position, ultimate ? ULTIMATE_EFFECTS[ring] : SKILL_EFFECTS[ring], ultimate ? 12 - ring * 1.6f : 7 - ring, ultimate ? 1.4f : .8f);
        }
        return true;
    }
}
