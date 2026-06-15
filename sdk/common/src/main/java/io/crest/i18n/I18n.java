package io.crest.i18n;

import java.lang.annotation.*;

@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// 定义当前业务对象的核心职责
public @interface I18n {
    String value() default "";
}
