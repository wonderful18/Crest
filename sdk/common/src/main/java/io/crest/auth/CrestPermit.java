package io.crest.auth;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 定义当前业务对象的核心职责
public @interface CrestPermit {

    /**
     * 鉴权el表达式数组
     * DE产品中仅考虑权限的&运算，不考虑||
     * @return
     */
    String[] value() default {};

    String busiFlag() default "";
}
