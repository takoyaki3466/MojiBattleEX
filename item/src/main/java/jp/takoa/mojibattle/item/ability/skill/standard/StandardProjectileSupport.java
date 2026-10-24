package jp.takoa.mojibattle.item.ability.skill.standard;

import jp.takoa.mojibattle.battle.action.spec.ProjectileShotSpec;
import jp.takoa.mojibattle.battle.status.StatusDuration;
import jp.takoa.mojibattle.battle.status.StatusEffectType;
import jp.takoa.mojibattle.battle.status.StatusSpec;
import jp.takoa.mojibattle.battle.status.StatusStacking;
import jp.takoa.mojibattle.resource.ResourceId;

import java.util.ArrayList;
import java.util.List;

/**
 * 標準投射スキル間で共有する、値オブジェクト生成だけの補助処理。
 */
final class StandardProjectileSupport {
    private StandardProjectileSupport() {
    }

    static ArrayList<ProjectileShotSpec> shots(List<String> glyphs, List<String> textures, List<List<Float>> offsets, List<Float> damages, List<Float> binds, float baseDamage, float stunSeconds, float bindSeconds) {
        ArrayList<ProjectileShotSpec> shots = new ArrayList<>(glyphs.size());
        for (int index = 0; index < glyphs.size(); index++) {
            List<Float> offset = index < offsets.size() ? offsets.get(index) : List.of();
            shots.add(new ProjectileShotSpec(glyphs.get(index), index < textures.size() ? resource(textures.get(index)) : null, number(offset, 0, 0f), number(offset, 1, 0f), number(damages, index, baseDamage), stunSeconds, number(binds, index, bindSeconds)));
        }
        return shots;
    }

    static void requireProjectileShape(List<String> glyphs, List<String> textures, List<List<Float>> offsets) {
        if (glyphs.isEmpty() || glyphs.size() > 64 || textures.size() != glyphs.size() || offsets.size() != glyphs.size() || offsets.stream().anyMatch(offset -> offset.size() < 2)) {
            throw new IllegalArgumentException("Projectile glyph, texture, and offset lists must have matching sizes");
        }
    }

    static StatusSpec status(String path, StatusEffectType type, float magnitude, float duration) {
        return new StatusSpec(new ResourceId("mojibattle", path), type, magnitude, StatusDuration.seconds(duration), StatusStacking.REFRESH, 1, 1f);
    }

    static ResourceId resource(String value) {
        return ResourceId.parse(value);
    }

    private static float number(List<Float> values, int index, float fallback) {
        return index < values.size() ? values.get(index) : fallback;
    }
}
