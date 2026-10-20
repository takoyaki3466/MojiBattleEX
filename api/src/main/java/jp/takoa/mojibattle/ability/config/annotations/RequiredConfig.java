package jp.takoa.mojibattle.ability.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 既定値へフォールバックせず、JSONでの明示指定を必須にする。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface RequiredConfig {
}
