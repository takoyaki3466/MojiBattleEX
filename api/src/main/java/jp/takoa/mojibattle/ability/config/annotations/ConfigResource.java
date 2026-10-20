package jp.takoa.mojibattle.ability.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** ResourceId設定の既定値を宣言する。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ConfigResource {
    String defaultValue();
    boolean nullable() default false;
    String name() default "";
}
