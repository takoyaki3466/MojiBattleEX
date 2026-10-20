package jp.takoa.mojibattle.config;

/** ウィンドウ、カメラ追従および視野角に関する描画設定DTO。 */
public final class GraphicsConfig {
    public int windowWidth;
    public int windowHeight;
    public boolean fullscreen;
    public float cameraDistance;
    public float cameraHeight;
    public float cameraFollowSpeed;
    public float fieldOfView;
    public boolean ultimateCutIns = true;
}
