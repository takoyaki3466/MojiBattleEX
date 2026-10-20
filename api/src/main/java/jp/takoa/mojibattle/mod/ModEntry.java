package jp.takoa.mojibattle.mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GameModのエントリーポイントを宣言する。
 * MODローダーはこのアノテーションからMOD IDを取得する。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModEntry {

    /**
     * @return MODの一意な識別子
     */
    String value();
}