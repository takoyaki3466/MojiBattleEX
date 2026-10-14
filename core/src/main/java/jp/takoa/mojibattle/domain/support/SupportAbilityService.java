package jp.takoa.mojibattle.domain.support;

import jp.takoa.mojibattle.audio.BattleSoundCue;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.SupportAbilityImplementation;

/**
 * 登録済み補助能力を解決し、発動条件・消費・共通演出だけを制御する。
 */
public final class SupportAbilityService {
    private static final String[] SKILL_EFFECTS = {EffectIds.WIND, EffectIds.KANA};
    private static final String[] ULTIMATE_EFFECTS = {EffectIds.STAR, EffectIds.FAMILY, EffectIds.NEUTRAL, EffectIds.STOCK};
    private final Registry<SupportAbilityImplementation> implementations;

    public SupportAbilityService(Registry<SupportAbilityImplementation> implementations) {
        this.implementations = implementations;
    }

    /**
     * 補助スキルが使用可能でメインが行動可能なら、クールタイムを先に確定して実装を呼び出す。
     */
    public void useSkill(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        SupportAbilityDefinition ability = support.definition.skill;

        if (!support.skillReady() || team.main().isStunned()) {
            return;
        }

        support.startSkillCooldown(battle.supportSkillCooldown(ability.cooldownSeconds));
        execute(battle, team, support, ability, false);
    }

    /**
     * メインが行動可能かつ補助専用ゲージを消費できる場合だけ必殺を実行し、共通告知を出す。
     */
    public void useUltimate(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        SupportAbilityDefinition ability = support.definition.ultimate;

        if (team.main().isStunned() || !support.spendUltimate(ability.pointCost)) {
            return;
        }

        execute(battle, team, support, ability, true);
        battle.announceUltimate(team.main().playerId, ability.nameKey);
    }

    /**
     * 実装IDを解決して固有処理を呼び、補助能力共通の効果音と多重リング演出を重ねる。
     */
    private void execute(BattleController battle, PlayerTeam team, SupportCharacterRuntime support, SupportAbilityDefinition ability, boolean ultimate) {
        SupportAbilityImplementation implementation = implementations.require(ability.type);
        battle.emitSound(BattleSoundCue.SUPPORT);

        implementation.handler().accept(new SupportAbilityContext(battle, team, support, ability), ultimate);

        for (int ring = 0; ring < (ultimate ? 4 : 2); ring++) {
            battle.emitEffect(support.position, ultimate ? ULTIMATE_EFFECTS[ring] : SKILL_EFFECTS[ring], ultimate ? 12 - ring * 1.6f : 7 - ring, ultimate ? 1.4f : .8f);
        }
    }
}
