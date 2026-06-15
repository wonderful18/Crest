package io.crest.auth;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 定义当前业务对象的核心职责
public @interface CrestLinkPermit {

    String value() default "";

}
