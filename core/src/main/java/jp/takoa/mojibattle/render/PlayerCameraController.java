package jp.takoa.mojibattle.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.battle.presentation.CutInMotion;
import jp.takoa.mojibattle.config.GraphicsConfig;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.CutInState;

/** 操作キャラクターと注視対象を収めるようにPerspectiveCameraを滑らかに追従させる。 */
public final class PlayerCameraController {
    private final PerspectiveCamera camera;
    private final GraphicsConfig config;
    private final Vector3 desired = new Vector3();
    private final Vector3 focus = new Vector3();
    private final Vector3 forward = new Vector3();
    private final Vector3 right = new Vector3();

    private float shakePhase;

    public PlayerCameraController(GraphicsConfig config) {
        this.config = config;
        camera = new PerspectiveCamera(config.fieldOfView, 1, 1);
        camera.near = 0.1f;
        camera.far = 80;
    }
    public void update(CharacterRuntime self, CharacterRuntime opponent, float delta, float viewportWidth,
                       float viewportHeight, float impactPulse, CutInState cutIn) {
        if (cutIn != null) {
            updateCinematic(cutIn, opponent, delta, viewportWidth, viewportHeight, impactPulse);
            return;
        }
        Vector3 away = new Vector3(self.position).sub(opponent.position).set(self.position.x - opponent.position.x, 0, self.position.z - opponent.position.z);

        if (away.isZero()) {
            away.set(0, 0, -1);
        } else {
            away.nor();
        }

        desired.set(self.position).mulAdd(away, config.cameraDistance).add(0, config.cameraHeight, 0);
        camera.position.lerp(desired, 1f - (float)Math.exp(-config.cameraFollowSpeed * Math.max(0, delta)));
        camera.fieldOfView = config.fieldOfView;
        shakePhase += delta * 52f;
        camera.position.add(MathUtils.sin(shakePhase) * impactPulse * .14f, MathUtils.cos(shakePhase * 1.31f) * impactPulse * .1f, 0);
        camera.viewportWidth = Math.max(1, viewportWidth);
        camera.viewportHeight = Math.max(1, viewportHeight);
        camera.up.set(Vector3.Y);
        camera.lookAt(self.position.x, self.position.y + 1f, self.position.z);
        camera.update();
    }

    /** カットイン進行率から、発動者を主役にした一続きのカメラ軌道を生成する。 */
    private void updateCinematic(CutInState cutIn, CharacterRuntime opponent, float delta,
                                 float viewportWidth, float viewportHeight, float impactPulse) {
        CharacterRuntime source = cutIn.source();
        float progress = MathUtils.clamp(cutIn.progress(), 0f, 1f);
        float intensity = cutIn.spec().intensity();
        float eased = progress * progress * (3f - 2f * progress);
        float exitPull = MathUtils.clamp((progress - .72f) / .28f, 0f, 1f);

        focus.set(source.position).add(0, 1.15f, 0);
        forward.set(opponent.position).sub(source.position).set(opponent.position.x - source.position.x,
                0, opponent.position.z - source.position.z);
        if (forward.isZero(.001f)) {
            forward.set(source.facing).set(source.facing.x, 0, source.facing.z);
        }
        forward.nor();
        right.set(forward).crs(Vector3.Y).nor();

        CutInMotion motion = cutIn.spec().motion();
        if (motion == CutInMotion.SLASH_RUSH) {
            float angle = MathUtils.lerp(-62f, 54f, eased) * MathUtils.degreesToRadians;
            float distance = MathUtils.lerp(7.2f, 3.1f, MathUtils.sin(progress * MathUtils.PI));
            distance += exitPull * 7f;
            desired.set(focus)
                    .mulAdd(forward, -MathUtils.cos(angle) * distance)
                    .mulAdd(right, MathUtils.sin(angle) * distance)
                    .add(0, MathUtils.lerp(4.8f, 1.7f, MathUtils.sin(progress * MathUtils.PI)), 0);
        } else if (motion == CutInMotion.HORIZONTAL_CHARGE) {
            float lateral = MathUtils.lerp(-9f, 8f, eased);
            desired.set(focus).mulAdd(right, lateral).mulAdd(forward, -4.2f - exitPull * 5f)
                    .add(0, 1.1f + MathUtils.sin(progress * MathUtils.PI) * 1.2f, 0);
        } else {
            float angle = MathUtils.lerp(-105f, 85f, eased) * MathUtils.degreesToRadians;
            float distance = 5.2f + exitPull * 7f;
            desired.set(focus)
                    .mulAdd(forward, -MathUtils.cos(angle) * distance)
                    .mulAdd(right, MathUtils.sin(angle) * distance)
                    .add(0, 2.6f, 0);
        }

        camera.position.lerp(desired, 1f - (float)Math.exp(-18f * Math.max(0, delta)));
        float impactBeat = MathUtils.clamp(1f - Math.abs(progress - .72f) * 18f, 0f, 1f);
        shakePhase += delta * 78f;
        float shake = (impactPulse * .18f + impactBeat * .22f) * intensity;
        camera.position.mulAdd(right, MathUtils.sin(shakePhase) * shake)
                .add(0, MathUtils.cos(shakePhase * 1.37f) * shake, 0);
        camera.fieldOfView = config.fieldOfView - MathUtils.sin(progress * MathUtils.PI) * 13f * intensity
                + exitPull * 9f;
        camera.viewportWidth = Math.max(1, viewportWidth);
        camera.viewportHeight = Math.max(1, viewportHeight);
        camera.up.set(Vector3.Y);
        camera.lookAt(focus);
        camera.update();
    }
    public PerspectiveCamera camera() {
        return camera;
    }
}
