package jp.takoa.mojibattle.ability.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Stringまたはfloatの不変List設定を宣言する。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ConfigList {
    ValueType value();
    int dimensions() default 1;
    String name() default "";

    /** List要素として許可する型。 */
    enum ValueType {
        STRING,
        FLOAT
    }
}
