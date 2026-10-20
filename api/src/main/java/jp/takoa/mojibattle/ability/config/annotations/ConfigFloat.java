package jp.takoa.mojibattle.ability.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** float設定の既定値と有効範囲を宣言する。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ConfigFloat {
    float defaultValue();
    float min() default 0f;
    float max() default Float.POSITIVE_INFINITY;
    boolean exclusiveMin() default false;
    boolean exclusiveMax() default false;
    String name() default "";
}
