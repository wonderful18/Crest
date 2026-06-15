package io.crest.auth;

import io.crest.constant.AuthResourceEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
// 定义当前业务对象的核心职责
public @interface CrestApiPath {

    @AliasFor("path")
    String[] value() default {};

    @AliasFor("value")
    String[] path() default {};

    AuthResourceEnum rt();
}
