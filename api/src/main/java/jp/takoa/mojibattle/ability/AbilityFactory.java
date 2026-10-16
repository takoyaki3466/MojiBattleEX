package jp.takoa.mojibattle.ability;

/**
 * Ability JSONのparametersから、プレイヤー専用の実行インスタンスを生成する。
 * @param <T> 生成するRuntime型
 */
@FunctionalInterface
public interface AbilityFactory<T> {
    /**
     * @param parameters JSONの{@code parameters}。未指定時は空View
     * @return 新しい装備者専用Runtime。{@code null}不可
     */
    T create(ConfigView parameters);
}
