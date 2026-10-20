package jp.takoa.mojibattle.ability.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** String設定の既定値と空文字の許可を宣言する。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ConfigString {
    String defaultValue();
    boolean allowBlank() default false;
    String name() default "";
}
