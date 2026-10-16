package jp.takoa.mojibattle.config;

/** 設定ファイルから読み込むフィールド効果1件の識別子、対象および数値パラメータ。 */
public final class FieldEffectDefinition {
    public String id;
    public String type;
    public String target;
    public float magnitude;
    public float intervalSeconds;
    public float durationSeconds;
    public float maximum;
    /** 対象に含めるキャラクターID。空配列は制限なし。 */
    public String[] includeCharacterIds = new String[0];
    /** 対象から除外するキャラクターID。includeより優先する。 */
    public String[] excludeCharacterIds = new String[0];
    public int priority;
}
