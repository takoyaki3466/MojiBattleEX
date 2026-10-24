package jp.takoa.mojibattle.domain.status;

/**
 * 同種の状態効果を再付与した際の残り時間・効果量の扱い。
 */
public enum StatusStackRule {
    REFRESH,
    STACK,
    REPLACE
}
