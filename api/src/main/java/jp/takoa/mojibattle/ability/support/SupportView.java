package jp.takoa.mojibattle.ability.support;

/**
 * 補助Abilityの所有個体と専用必殺ゲージを表す読取り専用ビュー。
 * HPや位置など共通の戦闘値は{@link jp.takoa.mojibattle.battle.combatant.BattlefieldView}から取得する。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext}と
 * {@link jp.takoa.mojibattle.ability.context.SupportPassiveContext}に含まれる。
 * ホストの補助個体アダプターが実装し、補助Runtimeから参照される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * boolean ready = support.ultimatePoints() >= support.maximumUltimatePoints();
 * String stableKey = support.instanceId();
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>補助ゲージという固有情報を一般の{@code CombatantView}へ押し込まず、補助能力に必要な最小情報だけを公開する。</p>
 */
public interface SupportView {

    String id();

    /**
     * 戦闘内で一意な個体ID。同じ定義を複数編成しても異なる。
     */
    default String instanceId() {
        return id();
    }

    float ultimatePoints();

    float maximumUltimatePoints();
}
