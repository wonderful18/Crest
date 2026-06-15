package io.crest.traffic;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 定义当前业务对象的核心职责
public @interface CrestTraffic {
    int value() default 0;

    String api();
}
