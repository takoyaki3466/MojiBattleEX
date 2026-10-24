package jp.takoa.mojibattle.battle.action.spec;

import jp.takoa.mojibattle.resource.ResourceId;

import java.util.List;

/**
 * 複数種類の弾を一括生成する、データ駆動の投射物編隊仕様。
 *
 * <h2>関係</h2>
 * <p>編隊要素の{@link ProjectileShotSpec}を保持し、任意で追撃の{@link FallingStrikeSpec}と
 * 各種{@link ResourceId}を参照する。{@link jp.takoa.mojibattle.battle.action.CombatActions#spawnProjectilePattern}へ渡され、
 * ホストの編隊配置・追尾・折返し・命中処理から依存される。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * ProjectilePatternSpec pattern = new ProjectilePatternSpec(
 *         List.of(centerShot, leftShot, rightShot), 9f, 0.3f, 4f,
 *         false, false, 0f, 2f, true, null, null, 0f, 0f, 0f,
 *         activationFx, hitFx, null, 1f, null, false);
 * actions.spawnProjectilePattern(user, pattern);
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>文字・画像・位置・個別ダメージの異なる弾を単一操作で同期生成し、複雑な編隊能力をcore非依存のデータとして記述するために作成された。</p>
 *
 * @param shots                 一体以上64体以下の弾要素
 * @param speed                 全弾共通の秒間速度
 * @param radius                全弾共通の衝突半径
 * @param lifetimeSeconds       全弾共通の最大生存秒
 * @param radial                放射状に発射するか
 * @param boomerang             一定距離で折り返すか
 * @param boomerangDistance     折返し距離
 * @param ultimatePointsPerHit  一命中ごとの必殺ポイント
 * @param pointsOnHit           命中時に必殺ポイントを付与するか
 * @param followUpGlyph         追撃表示用の任意文字
 * @param followUpTexture       追撃表示用の任意画像ID
 * @param followUpDamage        追撃の基礎ダメージ
 * @param followUpRadius        追撃の判定半径
 * @param followUpSpeed         追撃の速度
 * @param activationEffectId    任意の発動演出ID
 * @param hitEffectId           任意の命中演出ID
 * @param activationTexture     任意の発動時画像ID
 * @param activationEffectScale 発動演出倍率
 * @param fallingStrike         命中等に続ける任意の落下攻撃
 * @param ultimateAttack        必殺技種別の攻撃として扱うか
 */
public record ProjectilePatternSpec(List<ProjectileShotSpec> shots, float speed, float radius, float lifetimeSeconds,
                                    boolean radial, boolean boomerang, float boomerangDistance,
                                    float ultimatePointsPerHit, boolean pointsOnHit, String followUpGlyph,
                                    ResourceId followUpTexture, float followUpDamage, float followUpRadius,
                                    float followUpSpeed, ResourceId activationEffectId, ResourceId hitEffectId,
                                    ResourceId activationTexture, float activationEffectScale,
                                    FallingStrikeSpec fallingStrike, boolean ultimateAttack) {
    public ProjectilePatternSpec {
        shots = List.copyOf(shots);

        if (shots.isEmpty() || shots.size() > 64 || !positive(speed) || !positive(radius) || !positive(lifetimeSeconds) || !nonNegative(boomerangDistance) || !nonNegative(ultimatePointsPerHit) || !nonNegative(followUpDamage) || !nonNegative(followUpRadius) || !nonNegative(followUpSpeed) || !nonNegative(activationEffectScale)) {
            throw new IllegalArgumentException("Projectile pattern values are invalid");
        }
    }

    private static boolean positive(float value) {
        return Float.isFinite(value) && value > 0;
    }

    private static boolean nonNegative(float value) {
        return Float.isFinite(value) && value >= 0;
    }
}
