package jp.takoa.mojibattle.ability.support;

/**
 * 戦闘中の補助キャラクター一個体だけに属する、単純値用の型付き状態ストア。
 * 戦闘終了後は破棄され、参照してはならない。
 *
 * <h2>関係</h2>
 * <p>{@link jp.takoa.mojibattle.ability.context.SupportAbilityActivationContext}と
 * {@link jp.takoa.mojibattle.ability.context.SupportPassiveContext}から参照される。
 * ホストが実装と寿命管理を担当し、補助Runtimeがカウンターやフラグを保存する。</p>
 *
 * <h2>利用方法</h2>
 * <pre>{@code
 * int stacks = state.getInt("stacks", 0) + 1;
 * state.putInt("stacks", stacks);
 * if (stacks >= 3) state.remove("stacks");
 * }</pre>
 *
 * <h2>設計理由</h2>
 * <p>Runtimeがcoreのコンポーネントや永続ストレージへ依存せず、個体分離された短命な状態を安全に保持するために作成された。</p>
 */
public interface SupportStateStore {
    int getInt(String key, int fallback);
    void putInt(String key, int value);
    float getFloat(String key, float fallback);
    void putFloat(String key, float value);
    boolean getBoolean(String key, boolean fallback);
    void putBoolean(String key, boolean value);
    String getString(String key, String fallback);
    void putString(String key, String value);
    boolean contains(String key);
    void remove(String key);
}
