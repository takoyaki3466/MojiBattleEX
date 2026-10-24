package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.battle.damage.DamageRequest;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.Objects;

/**
 * 使用者が事前に設置した起爆可能マーカーをまとめて起爆する仕様。
 *
 * <h2>関係</h2>
 * <p>{@link DamageRequest}と複数の{@link ResourceId}に依存する。
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#spawnDetonatableMarker}で作成した所有マーカーを、
 * {@link jp.takoa.mojibattle.battle.action.CombatActions#detonateOwnedMarkers}が本仕様に従って消費する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * actions.detonateOwnedMarkers(user,
 *         new DetonationSpec(damage, 2.5f, 0.5f, 0.4f, 1.2f, 3f,
 *                 markerTexture, warningFx, impactFx));
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>設置と起爆を別能力から呼べるよう所有権をホストへ保持させ、起爆時の範囲・演出・被ダメージ増加を一つの値で渡すために作成された。</p>
 *
 * @param damage                   各対象へ適用するダメージ要求
 * @param radius                   マーカーごとの起爆半径
 * @param warningSeconds           起爆前の予告秒
 * @param effectSeconds            命中演出の表示秒
 * @param incomingDamageMultiplier 命中対象へ付与する被ダメージ倍率
 * @param debuffDurationSeconds    被ダメージ倍率の継続秒
 * @param texture                  マーカー画像ID
 * @param warningEffectId          任意の予告演出ID
 * @param impactEffectId           任意の起爆演出ID
 */
public record DetonationSpec(DamageRequest damage, float radius, float warningSeconds, float effectSeconds,
                             float incomingDamageMultiplier, float debuffDurationSeconds, ResourceId texture,
                             ResourceId warningEffectId, ResourceId impactEffectId) {
    public DetonationSpec {
        Objects.requireNonNull(damage, "damage");

        if (!positive(radius) || !nonNegative(warningSeconds) || !nonNegative(effectSeconds) || !positive(incomingDamageMultiplier) || !positive(debuffDurationSeconds)) {
            throw new IllegalArgumentException("Detonation values are invalid");
        }
    }

    private static boolean positive(float value) {
        return Float.isFinite(value) && value > 0;
    }

    private static boolean nonNegative(float value) {
        return Float.isFinite(value) && value >= 0;
    }
}
