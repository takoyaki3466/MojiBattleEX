package jp.takoa.mojibattle.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import jp.takoa.mojibattle.config.BattleConfig;
import jp.takoa.mojibattle.domain.combat.AwakeningItem;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.BattleTeamDefinition;
import jp.takoa.mojibattle.domain.combat.CarHazard;
import jp.takoa.mojibattle.domain.combat.CashBundle;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.combat.CombatObject;
import jp.takoa.mojibattle.domain.combat.FamilySummon;
import jp.takoa.mojibattle.domain.combat.LineStrike;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.domain.combat.SpinningOne;
import jp.takoa.mojibattle.domain.combat.UltimateStrike;
import jp.takoa.mojibattle.domain.combat.VisualEffect;
import jp.takoa.mojibattle.domain.support.SupportCharacterDefinition;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;
import jp.takoa.mojibattle.domain.ability.AbilityConfigs;
import jp.takoa.mojibattle.registry.CombatEffectDefinition;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.resource.GdxTextureLoader;
import jp.takoa.mojibattle.resource.ResourceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

/**
 * 3D戦場、キャラクター、攻撃オブジェクトおよび演出を1台のカメラから描画する。
 */
public final class ArenaRenderer implements Disposable {
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static final long TEXTURED_ATTRIBUTES = ATTRIBUTES | VertexAttributes.Usage.TextureCoordinates;

    private final ModelBatch batch = new ModelBatch();
    private final Environment environment = new Environment();
    private final GdxTextureLoader textureLoader;

    private final List<Model> models = new ArrayList<>();
    private final List<Texture> textures = new ArrayList<>();

    private final Map<String, ModelInstance> projectileGlyphs = new HashMap<>();
    private final Map<String, ModelInstance> projectileOutlines = new HashMap<>();
    private final Map<String, ModelInstance> effectModels = new HashMap<>();
    private final Map<String, ModelInstance> objectModels = new HashMap<>();

    private final Vector3 tempPosition = new Vector3();
    private final Vector3 tempDirection = new Vector3();
    private final Vector3 tempOutline = new Vector3();

    private final ModelInstance ground;
    private final EnumMap<PlayerId, ModelInstance> playerModels = new EnumMap<>(PlayerId.class);
    private final EnumMap<PlayerId, List<ModelInstance>> subMainModels = new EnumMap<>(PlayerId.class);
    private final EnumMap<PlayerId, List<ModelInstance>> supportModels = new EnumMap<>(PlayerId.class);

    private final ModelInstance familyHealthBack, familyHealthFill;
    private final ModelInstance[] walls = new ModelInstance[4];
    private final ModelInstance[] scenery = new ModelInstance[4], matsudoScenery = new ModelInstance[4];

    private float animationTime;

    public ArenaRenderer(BattleConfig config, List<BattleTeamDefinition> teamDefinitions,
                         List<AbilityConfigs.ProjectileSkill> skills,
                         List<AbilityConfigs.AreaUltimate> ultimates,
                         String awakeningTexturePath, Iterable<CombatEffectDefinition> effects,
                         ResourceManager resources) {
        textureLoader = new GdxTextureLoader(resources);
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.95f, 0.97f, 1f, 1));

        ModelBuilder modelBuilder = new ModelBuilder();

        Texture groundTexture = texture("arena/sparse-grass.jpg");
        groundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        TextureAttribute groundDiffuse = TextureAttribute.createDiffuse(groundTexture);
        groundDiffuse.scaleU = 7f;
        groundDiffuse.scaleV = 7f;

        ground = instance(modelBuilder.createBox(config.arenaHalfExtent * 2, 0.2f, config.arenaHalfExtent * 2, new Material(groundDiffuse, ColorAttribute.createDiffuse(Color.WHITE)), TEXTURED_ATTRIBUTES));
        Model wallModel = modelBuilder.createBox(config.arenaHalfExtent * 2 + 0.4f, 1.0f, 0.35f, material(new Color(0.58f, 0.72f, 0.68f, 1)), ATTRIBUTES);
        models.add(wallModel);

        for (int i = 0; i < walls.length; i++) {
            walls[i] = new ModelInstance(wallModel);
        }

        walls[0].transform.setToTranslation(0, .5f, -config.arenaHalfExtent);
        walls[1].transform.setToTranslation(0, .5f, config.arenaHalfExtent);
        walls[2].transform.setToTranslation(-config.arenaHalfExtent, .5f, 0).rotate(Vector3.Y, 90);
        walls[3].transform.setToTranslation(config.arenaHalfExtent, .5f, 0).rotate(Vector3.Y, 90);

        Texture sceneryTexture = texture("arena/hilly-terrain.jpg");
        float backdropDistance = config.arenaHalfExtent + 7f;

        scenery[0] = sceneryRect(modelBuilder, sceneryTexture, 46f, 19f);
        scenery[1] = sceneryRect(modelBuilder, sceneryTexture, 46f, 19f);
        scenery[2] = sceneryRect(modelBuilder, sceneryTexture, 46f, 19f);
        scenery[3] = sceneryRect(modelBuilder, sceneryTexture, 46f, 19f);

        // 法線はアリーナの外側を向いています。GL_BACKカリングにより、内側にいるプレイヤーからは草の平面が見えなくなります。
        scenery[0].transform.setToTranslation(0, 7.5f, -backdropDistance).rotate(Vector3.Y, 180);
        scenery[1].transform.setToTranslation(0, 7.5f, backdropDistance);
        scenery[2].transform.setToTranslation(-backdropDistance, 7.5f, 0).rotate(Vector3.Y, -90);
        scenery[3].transform.setToTranslation(backdropDistance, 7.5f, 0).rotate(Vector3.Y, 90);

        Texture matsudoTexture = texture("arena/matsudo-backdrop.png");

        for (int index = 0; index < matsudoScenery.length; index++) {
            matsudoScenery[index] = sceneryRect(modelBuilder, matsudoTexture, 46f, 19f);
        }

        matsudoScenery[0].transform.setToTranslation(0, 7.5f, -backdropDistance).rotate(Vector3.Y, 180);
        matsudoScenery[1].transform.setToTranslation(0, 7.5f, backdropDistance);
        matsudoScenery[2].transform.setToTranslation(-backdropDistance, 7.5f, 0).rotate(Vector3.Y, -90);
        matsudoScenery[3].transform.setToTranslation(backdropDistance, 7.5f, 0).rotate(Vector3.Y, 90);

        Color[] playerColors = {new Color(.2f, .65f, 1, 1), new Color(1, .35f, .28f, 1), new Color(.45f, 1, .35f, 1), new Color(1, .45f, 1, 1)};
        for (int index = 0; index < teamDefinitions.size(); index++) {
            PlayerId id = PlayerId.values()[index];
            BattleTeamDefinition team = teamDefinitions.get(index);
            playerModels.put(id, texturedRect(modelBuilder, texture(team.main().glyphTexture), 1.8f, 2.8f, playerColors[index]));
            List<ModelInstance> subModelsForPlayer = new ArrayList<>();
            for (CharacterDefinition subMain : team.subMains()) {
                subModelsForPlayer.add(texturedRect(modelBuilder, texture(subMain.glyphTexture), 1.55f, 2.35f, playerColors[index]));
            }
            subMainModels.put(id, subModelsForPlayer);
            List<ModelInstance> modelsForPlayer = new ArrayList<>();
            for (SupportCharacterDefinition support : team.supports()) {
                if (support != null) {
                    modelsForPlayer.add(texturedRect(modelBuilder, texture(support.glyphTexture), 1.2f, 1.8f, playerColors[index]));
                }
            }
            supportModels.put(id, modelsForPlayer);
        }
        familyHealthBack = instance(modelBuilder.createBox(1.65f, .14f, .07f, material(new Color(.28f, .04f, .04f, 1)), ATTRIBUTES));
        familyHealthFill = instance(modelBuilder.createBox(1.58f, .09f, .08f, material(new Color(.18f, 1f, .30f, 1)), ATTRIBUTES));

        Color[] projectileColors = {new Color(1, 0.78f, 0.2f, 1), new Color(1, 0.55f, 0.15f, 1), new Color(1, 0.88f, 0.3f, 1), new Color(1, 0.65f, 0.1f, 1)};
        int colorIndex = 0;

        for (AbilityConfigs.ProjectileSkill skill : skills) {
            for (String texturePath : skill.textures) {
                if (!projectileGlyphs.containsKey(texturePath)) {
                    Texture glyphTexture = texture(texturePath);
                    projectileGlyphs.put(texturePath, texturedRect(modelBuilder, glyphTexture, .8f, .8f, projectileColors[colorIndex++ % projectileColors.length]));
                    projectileOutlines.put(texturePath, texturedRect(modelBuilder, glyphTexture, .8f, .8f, Color.BLACK));
                    objectModels.putIfAbsent(texturePath, new ModelInstance(projectileGlyphs.get(texturePath).model));
                }
            }
        }
        for (BattleTeamDefinition team : teamDefinitions) {
            for (SupportCharacterDefinition supportDefinition : team.supports()) {
                if (supportDefinition == null) {
                    continue;
                }
                String texturePath = supportDefinition.projectileTexture;

                if (!projectileGlyphs.containsKey(texturePath)) {
                    Texture glyphTexture = texture(texturePath);
                    projectileGlyphs.put(texturePath, texturedRect(modelBuilder, glyphTexture, .8f, .8f, Color.WHITE));
                    projectileOutlines.put(texturePath, texturedRect(modelBuilder, glyphTexture, .8f, .8f, Color.BLACK));
                    objectModels.putIfAbsent(texturePath, new ModelInstance(projectileGlyphs.get(texturePath).model));
                }
            }
        }
        for (CombatEffectDefinition effect : effects) {
            effectModels.put(effect.id(), effectRect(modelBuilder, texture(effect.texturePath())));
        }

        for (AbilityConfigs.AreaUltimate ultimate : ultimates) {
            addObjectModel(modelBuilder, ultimate.objectTexture);
        }
        for (AbilityConfigs.ProjectileSkill skill : skills) {
            addObjectModel(modelBuilder, skill.activationTexture);
            addObjectModel(modelBuilder, skill.fallingObjectTexture);
        }
        for (BattleTeamDefinition team : teamDefinitions) {
            addProjectileModel(modelBuilder, team.main().glyphTexture, Color.WHITE);
            for (CharacterDefinition subMain : team.subMains()) {
                addProjectileModel(modelBuilder, subMain.glyphTexture, Color.WHITE);
            }
        }
        addObjectModel(modelBuilder, "objects/commuter-train.png");

        Texture awakening = texture(awakeningTexturePath);
        objectModels.put(awakeningTexturePath, texturedRect(modelBuilder, awakening, (float) awakening.getWidth() / awakening.getHeight(), 1, Color.WHITE));
    }

    private ModelInstance instance(Model model) {
        models.add(model);
        return new ModelInstance(model);
    }

    private void addObjectModel(ModelBuilder builder, String texturePath) {
        if (texturePath == null || objectModels.containsKey(texturePath)) {
            return;
        }
        Texture objectTexture = texture(texturePath);
        objectModels.put(texturePath, texturedRect(builder, objectTexture, Math.max(.5f, (float) objectTexture.getWidth() / objectTexture.getHeight()), 1f, Color.WHITE));
    }

    private void addProjectileModel(ModelBuilder builder, String texturePath, Color tint) {
        if (texturePath == null || projectileGlyphs.containsKey(texturePath)) {
            return;
        }
        Texture glyphTexture = texture(texturePath);
        projectileGlyphs.put(texturePath, texturedRect(builder, glyphTexture, .8f, .8f, tint));
        projectileOutlines.put(texturePath, texturedRect(builder, glyphTexture, .8f, .8f, Color.BLACK));
    }

    private static Material material(Color color) {
        return new Material(ColorAttribute.createDiffuse(color));
    }

    private Texture texture(String path) {
        Texture value = textureLoader.load(path, true);
        value.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        textures.add(value);

        return value;
    }

    private ModelInstance texturedRect(ModelBuilder builder, Texture texture, float width, float height, Color tint) {
        Material material = new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createDiffuse(tint), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA), IntAttribute.createCullFace(GL20.GL_NONE));
        float x = width / 2, y = height / 2;

        return instance(builder.createRect(-x, -y, 0, x, -y, 0, x, y, 0, -x, y, 0, 0, 0, 1, material, TEXTURED_ATTRIBUTES));
    }

    private ModelInstance effectRect(ModelBuilder builder, Texture texture) {
        Material material = new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createDiffuse(Color.WHITE), new BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE, 1f), IntAttribute.createCullFace(GL20.GL_NONE));

        return instance(builder.createRect(-.5f, -.5f, 0, .5f, -.5f, 0, .5f, .5f, 0, -.5f, .5f, 0, 0, 0, 1, material, TEXTURED_ATTRIBUTES));
    }

    private ModelInstance sceneryRect(ModelBuilder builder, Texture texture, float width, float height) {
        Material material = new Material(TextureAttribute.createDiffuse(texture), ColorAttribute.createDiffuse(Color.WHITE), IntAttribute.createCullFace(GL20.GL_BACK));
        float x = width / 2, y = height / 2;

        return instance(builder.createRect(-x, -y, 0, x, -y, 0, x, y, 0, -x, y, 0, 0, 0, 1, material, TEXTURED_ATTRIBUTES));
    }

    public void updateAnimation(float delta) {
        animationTime += Math.max(0, Math.min(delta, .1f));
    }

    public void render(Camera camera, BattleController battle) {
        batch.begin(camera);
        ModelInstance[] activeScenery = battle.fieldEffectIds().contains("matsudo") ? matsudoScenery : scenery;

        for (ModelInstance backdrop : activeScenery) {
            batch.render(backdrop, environment);
        }

        ground.transform.setToTranslation(0, -0.1f, 0);
        batch.render(ground, environment);

        for (ModelInstance wall : walls) {
            batch.render(wall, environment);
        }

        for (PlayerId id : battle.playerIds()) {
            if (battle.player(id).isAlive()) {
                renderCharacter(playerModels.get(id), battle.player(id), camera);
                List<CharacterRuntime> subMains = battle.subMains(id);
                List<ModelInstance> mainModels = subMainModels.get(id);
                for (int subIndex = 0; subIndex < subMains.size(); subIndex++) {
                    renderCharacter(mainModels.get(subIndex), subMains.get(subIndex), camera);
                }
                List<SupportCharacterRuntime> supports = battle.supports(id);
                List<ModelInstance> models = supportModels.get(id);
                for (int supportIndex = 0; supportIndex < supports.size(); supportIndex++) {
                    renderSupport(models.get(supportIndex), supports.get(supportIndex), camera);
                }
            }
        }
        for (CombatObject object : battle.objects()) {
            if (object instanceof Projectile p) {
                float phase = animationTime * 13f + p.attackId * .73f;
                float pulse = 1.15f + MathUtils.sin(phase) * .28f;

                ModelInstance aura = effectModels.get(p.effectId);
                billboard(aura, p.position(), camera, pulse * 4.1f);
                aura.transform.rotate(Vector3.Z, animationTime * 260f + p.attackId * 19f);
                batch.render(aura, environment);

                billboard(aura, p.position(), camera, pulse * 2.3f);
                aura.transform.rotate(Vector3.Z, -animationTime * 390f - p.attackId * 11f);
                batch.render(aura, environment);

                ModelInstance glyph = projectileGlyphs.get(p.texturePath);
                if (glyph != null) {
                    tempPosition.set(p.position()).add(0, MathUtils.sin(phase * .7f) * .16f, 0);

                    ModelInstance outline = projectileOutlines.get(p.texturePath);
                    tempDirection.set(camera.position).sub(tempPosition).nor();
                    tempOutline.set(tempPosition).mulAdd(tempDirection, -.035f);

                    billboard(outline, tempOutline, camera, 3.5f + MathUtils.sin(phase) * .28f);
                    outline.transform.rotate(Vector3.Z, MathUtils.sin(phase * .45f) * 18f);
                    batch.render(outline, environment);

                    billboard(glyph, tempPosition, camera, 2.76f + MathUtils.sin(phase) * .28f);
                    glyph.transform.rotate(Vector3.Z, p.texturePath.contains("spoon") ? animationTime * 720f : MathUtils.sin(phase * .45f) * 18f);
                    batch.render(glyph, environment);
                }
            } else if (object instanceof CashBundle cash) {
                ModelInstance image = objectModels.get(cash.texturePath);
                if (image != null) {
                    tempPosition.set(cash.position()).add(0, .55f + MathUtils.sin(animationTime * 5f) * .08f, 0);
                    billboard(image, tempPosition, camera, 2.4f);
                    image.transform.rotate(Vector3.Z, MathUtils.sin(animationTime * 4f) * 7f);
                    batch.render(image, environment);
                }
            } else if (object instanceof UltimateStrike strike) {
                if (strike.phase() == UltimateStrike.Phase.PENDING) {
                    continue;
                }
                float warningPulse = 1f + MathUtils.sin(animationTime * 9f) * .12f;

                ModelInstance warning = effectModels.get(strike.warningEffectId);
                warning.transform.setToTranslation(strike.position().x, .04f, strike.position().z).rotate(Vector3.Y, animationTime * 95f).rotate(Vector3.X, -90).scale(strike.radius * 5f * warningPulse, strike.radius * 5f * warningPulse, 1);
                batch.render(warning, environment);

                float remaining = 1 - strike.fallProgress();
                float easedRemaining = remaining * remaining;
                float y = strike.phase() == UltimateStrike.Phase.WARNING ? strike.spawnHeight * easedRemaining + 2 : 2;
                float side = strike.owner.ordinal() % 2 == 0 ? -1 : 1;

                tempPosition.set(strike.position()).add(strike.fallHorizontalOffset * easedRemaining * side, y, -strike.fallHorizontalOffset * easedRemaining * .3f);
                ModelInstance image = objectModels.get(strike.objectTexture);

                if (image != null) {
                    float fallScale = Math.max(4f, strike.radius * 1.4f) * (1f + MathUtils.sin(animationTime * 11f) * .04f);

                    billboard(image, tempPosition, camera, fallScale);
                    image.transform.rotate(Vector3.Z, side * 22f + animationTime * side * 85f);
                    batch.render(image, environment);
                }
                if (strike.phase() == UltimateStrike.Phase.IMPACT) {
                    float impactScale = strike.radius * (2.8f + MathUtils.sin(animationTime * 18f) * .45f);

                    ModelInstance impact = effectModels.get(strike.impactEffectId);
                    billboard(impact, tempPosition.set(strike.position()).add(0, 2f, 0), camera, impactScale);
                    impact.transform.rotate(Vector3.Z, animationTime * 310f);
                    batch.render(impact, environment);

                    billboard(impact, tempPosition, camera, impactScale * .62f);
                    impact.transform.rotate(Vector3.Z, -animationTime * 430f);
                    batch.render(impact, environment);
                }
            } else if (object instanceof VisualEffect effect) {
                ModelInstance visual = effect.texturePath == null ? effectModels.get(effect.effectId) : objectModels.get(effect.texturePath);

                if (visual == null) {
                    visual = effectModels.get(effect.effectId);
                }

                if (visual == null) {
                    continue;
                }

                float pulse = 1f + MathUtils.sin(animationTime * 16f + effect.effectId.hashCode()) * .14f;
                float scale = effect.scale() * pulse;

                tempPosition.set(effect.position()).add(0, 1f + effect.progress() * .6f, 0);
                billboard(visual, tempPosition, camera, scale);
                visual.transform.rotate(Vector3.Z, animationTime * 280f + effect.effectId.hashCode() % 360);
                batch.render(visual, environment);

                billboard(visual, tempPosition, camera, scale * .58f);
                visual.transform.rotate(Vector3.Z, -animationTime * 410f);
                batch.render(visual, environment);

            } else if (object instanceof LineStrike line) {
                renderLineStrike(line, camera);

            } else if (object instanceof FamilySummon family) {
                renderFamily(family, camera);

            } else if (object instanceof CarHazard car) {
                renderCar(car, camera);

            } else if (object instanceof SpinningOne one) {
                renderSpinningOne(one, camera);

            } else if (object instanceof AwakeningItem item) {
                ModelInstance image = objectModels.get(item.texturePath);

                if (image == null) {
                    continue;
                }
                tempPosition.set(item.position()).add(0, 1.4f + MathUtils.sin(animationTime * 5) * .25f, 0);
                billboard(image, tempPosition, camera, 3.8f);

                image.transform.rotate(Vector3.Z, animationTime * 120f);
                batch.render(image, environment);
            }
        }
        batch.end();
    }

    private void renderCharacter(ModelInstance model, CharacterRuntime runtime, Camera camera) {
        model.transform.setToTranslation(runtime.position.x, runtime.position.y + 1.4f, runtime.position.z);
        float yaw = (float) Math.toDegrees(Math.atan2(runtime.facing.x, runtime.facing.z));

        model.transform.rotate(Vector3.Y, yaw);
        batch.render(model, environment);

        if (runtime.awakened()) {
            ModelInstance aura = effectModels.get(EffectIds.FAMILY);
            tempPosition.set(runtime.position).add(0, 1.45f, 0);
            billboard(aura, tempPosition, camera, 3.8f + MathUtils.sin(animationTime * 9f) * .35f);

            aura.transform.rotate(Vector3.Z, animationTime * 210f);
            batch.render(aura, environment);
        }
        if (runtime.isStunned()) {
            ModelInstance stun = effectModels.get(EffectIds.VEHICLE);
            tempPosition.set(runtime.position).add(0, 3.15f + MathUtils.sin(animationTime * 8f) * .18f, 0);
            billboard(stun, tempPosition, camera, 1.25f + MathUtils.sin(animationTime * 12f) * .15f);

            stun.transform.rotate(Vector3.Z, animationTime * 240f);
            batch.render(stun, environment);
        }
    }

    private void renderSupport(ModelInstance model, SupportCharacterRuntime runtime, Camera camera) {
        tempPosition.set(runtime.position).add(0, 1.15f + MathUtils.sin(animationTime * 6f) * .14f, 0);
        billboard(model, tempPosition, camera, 1.25f);
        batch.render(model, environment);
    }

    private void renderLineStrike(LineStrike line, Camera camera) {
        tempPosition.set(line.position()).mulAdd(line.direction, line.length * .5f).add(0, .08f, 0);

        float yaw = -(float) Math.toDegrees(Math.atan2(line.direction.z, line.direction.x));
        float warningPulse = 1f + MathUtils.sin(animationTime * 14f) * .16f;

        ModelInstance warning = effectModels.get(line.warningEffectId);
        warning.transform.setToTranslation(tempPosition).rotate(Vector3.Y, yaw).rotate(Vector3.X, -90).scale(line.length * 1.3f, line.width * 5.2f * warningPulse, 1);
        batch.render(warning, environment);

        if (line.impacting()) {
            ModelInstance lance = objectModels.get(line.objectTexture);
            float surge = 1f + MathUtils.sin(animationTime * 22f) * .1f;

            lance.transform.setToTranslation(tempPosition.x, 1.2f + MathUtils.sin(animationTime * 18f) * .12f, tempPosition.z).rotate(Vector3.Y, yaw).scale(line.length * 1.15f * surge, line.width * 4.4f * surge, 1);
            batch.render(lance, environment);
        }
    }

    private void renderCar(CarHazard car, Camera camera) {
        ModelInstance image = objectModels.get(car.texturePath);
        if (image == null) {
            return;
        }
        float speedPulse = 1f + MathUtils.sin(animationTime * 28f) * .06f;

        tempPosition.set(car.position()).add(0, 1.4f + MathUtils.sin(animationTime * 20f) * .14f, 0);
        billboard(image, tempPosition, camera, car.radius * 3.2f * speedPulse);
        image.transform.rotate(Vector3.Z, MathUtils.sin(animationTime * 18f) * 5f);
        batch.render(image, environment);

        ModelInstance trail = effectModels.get(car.effectId);

        tempPosition.mulAdd(car.velocity, -.09f);
        billboard(trail, tempPosition, camera, car.radius * 4.5f);
        trail.transform.rotate(Vector3.Z, animationTime * 420f);
        batch.render(trail, environment);
    }

    private void renderSpinningOne(SpinningOne one, Camera camera) {
        ModelInstance image = objectModels.get(one.texturePath);
        if (image == null) {
            return;
        }

        float pulse = 1f + MathUtils.sin(animationTime * 24f) * .08f;

        tempPosition.set(one.position()).add(0, .16f, 0);
        image.transform.setToTranslation(tempPosition).rotate(Vector3.Y, one.angle() - 90f).rotate(Vector3.X, -90f).scale(one.length * pulse, one.length * pulse, one.length * pulse);
        batch.render(image, environment);

        ModelInstance aura = effectModels.get(one.effectId);
        aura.transform.setToTranslation(one.position().x, .12f, one.position().z).rotate(Vector3.Y, -one.angle() * 1.7f).rotate(Vector3.X, -90f).scale(one.length * 1.18f * pulse, one.length * 1.18f * pulse, one.length * 1.18f * pulse);
        batch.render(aura, environment);
    }

    private void renderFamily(FamilySummon family, Camera camera) {
        float pulse = 1f + MathUtils.sin(animationTime * 9f + family.orbitAngle()) * .08f;

        tempPosition.set(family.position()).add(0, 1.15f, 0);

        ModelInstance outline = projectileOutlines.get(family.texturePath);
        ModelInstance glyph = projectileGlyphs.get(family.texturePath);

        if (outline != null) {
            tempDirection.set(camera.position).sub(tempPosition).nor();
            tempOutline.set(tempPosition).mulAdd(tempDirection, -.035f);

            billboard(outline, tempOutline, camera, 1.9f * pulse);
            batch.render(outline, environment);
        }
        if (glyph != null) {
            billboard(glyph, tempPosition, camera, 1.55f * pulse);
            batch.render(glyph, environment);
        }
        tempPosition.set(family.position()).add(0, 2.35f, 0);
        tempDirection.set(camera.position).sub(tempPosition);

        float yaw = (float) Math.toDegrees(Math.atan2(tempDirection.x, tempDirection.z));

        familyHealthBack.transform.setToTranslation(tempPosition).rotate(Vector3.Y, yaw);
        batch.render(familyHealthBack, environment);

        tempOutline.set(tempPosition).mulAdd(tempDirection.nor(), .025f);
        familyHealthFill.transform.setToTranslation(tempOutline).rotate(Vector3.Y, yaw).scale(family.hpRatio(), 1, 1);
        batch.render(familyHealthFill, environment);
    }

    private void billboard(ModelInstance model, Vector3 position, Camera camera, float scale) {
        tempDirection.set(camera.position).sub(position);
        float yaw = (float) Math.toDegrees(Math.atan2(tempDirection.x, tempDirection.z));

        model.transform.setToTranslation(position).rotate(Vector3.Y, yaw).scale(scale, scale, 1);
    }

    @Override
    public void dispose() {
        batch.dispose();
        for (Model model : models) {
            model.dispose();
        }
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
