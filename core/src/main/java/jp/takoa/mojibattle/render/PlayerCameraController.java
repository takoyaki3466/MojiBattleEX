package jp.takoa.mojibattle.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.config.GraphicsConfig;
import jp.takoa.mojibattle.domain.CharacterRuntime;

public final class PlayerCameraController {
    private final PerspectiveCamera camera;
    private final GraphicsConfig config;
    private final Vector3 desired = new Vector3();
    private float shakePhase;

    public PlayerCameraController(GraphicsConfig config) {
        this.config = config;
        camera = new PerspectiveCamera(config.fieldOfView, 1, 1);
        camera.near = 0.1f; camera.far = 80;
    }
    public void update(CharacterRuntime self, CharacterRuntime opponent, float delta, float viewportWidth, float viewportHeight, float impactPulse) {
        Vector3 away = new Vector3(self.position).sub(opponent.position).set(self.position.x - opponent.position.x, 0, self.position.z - opponent.position.z);
        if (away.isZero()) away.set(0, 0, -1); else away.nor();
        desired.set(self.position).mulAdd(away, config.cameraDistance).add(0, config.cameraHeight, 0);
        camera.position.lerp(desired, 1f - (float)Math.exp(-config.cameraFollowSpeed * Math.max(0, delta)));
        shakePhase += delta * 52f;
        camera.position.add(MathUtils.sin(shakePhase) * impactPulse * .14f, MathUtils.cos(shakePhase * 1.31f) * impactPulse * .1f, 0);
        camera.viewportWidth = Math.max(1, viewportWidth); camera.viewportHeight = Math.max(1, viewportHeight);
        camera.up.set(Vector3.Y);
        camera.lookAt(self.position.x, self.position.y + 1f, self.position.z);
        camera.update();
    }
    public PerspectiveCamera camera() { return camera; }
}
