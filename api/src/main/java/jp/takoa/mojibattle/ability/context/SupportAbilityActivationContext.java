package jp.takoa.mojibattle.ability.context;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.support.SupportActions;
import jp.takoa.mojibattle.ability.support.SupportStateStore;
import jp.takoa.mojibattle.ability.support.SupportView;
import jp.takoa.mojibattle.battle.action.CombatActions;
import jp.takoa.mojibattle.battle.combatant.CombatantView;

import java.util.Objects;

/**
 * 補助キャラクターのスキルまたは必殺技へ渡す発動コンテキスト。
 *
 * <h2>関係</h2>
 * <p>{@link SupportView}で補助個体、{@link CombatantView}で所有者と対象を公開する。
 * 一般戦闘操作は{@link CombatActions}、補助個体固有の操作は{@link SupportActions}、
 * 発動をまたぐ状態は{@link SupportStateStore}へ分離されている。
 * {@link jp.takoa.mojibattle.ability.runtime.SupportAbilityRuntime}から利用され、ホストの補助Abilityアダプターが生成する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * if (context.target() != null) {
 *     context.actions().damage(context.owner(), context.target(), 12f);
 *     context.state().putInt("uses", context.state().getInt("uses", 0) + 1);
 * }
 * }</pre>
 *
 * <h2>互換性</h2>
 * <p>簡易コンストラクターは旧ホスト向けで、{@link #supportActions()}または{@link #state()}を使うと例外になる。
 * 新しいホストは全依存を渡すコンストラクターを使用する。</p>
 *
 * <h2>設計理由</h2>
 * <p>補助個体の権限とメイン戦闘操作を区別し、core内部型をAPI利用者へ露出させないために作成された。</p>
 */
public final class SupportAbilityActivationContext {
    private final SupportView support;
    private final CombatantView owner;
    private final CombatantView target;
    private final CombatActions actions;
    private final ConfigView config;
    private final float battleElapsedSeconds;
    private final SupportActions supportActions;
    private final SupportStateStore state;

    public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target, CombatActions actions, ConfigView config, float battleElapsedSeconds) {
        this(support, owner, target, actions, config, battleElapsedSeconds, null, null);
    }

    public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target, CombatActions actions, ConfigView config, float battleElapsedSeconds, SupportActions supportActions, SupportStateStore state) {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(config, "config");

        if (!Float.isFinite(battleElapsedSeconds) || battleElapsedSeconds < 0) {
            throw new IllegalArgumentException("battleElapsedSeconds must be finite and non-negative");
        }

        this.support = support;
        this.owner = owner;
        this.target = target;
        this.actions = actions;
        this.config = config;
        this.battleElapsedSeconds = battleElapsedSeconds;
        this.supportActions = supportActions;
        this.state = state;
    }

    public SupportView support() {
        return support;
    }

    public CombatantView owner() {
        return owner;
    }

    public CombatantView target() {
        return target;
    }

    public CombatActions actions() {
        return actions;
    }

    public ConfigView config() {
        return config;
    }

    public float battleElapsedSeconds() {
        return battleElapsedSeconds;
    }

    public SupportActions supportActions() {
        return Objects.requireNonNull(supportActions, "Host does not provide SupportActions");
    }

    public SupportStateStore state() {
        return Objects.requireNonNull(state, "Host does not provide SupportStateStore");
    }
}
