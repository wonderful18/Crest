package io.crest.log;

import io.crest.constant.LogOT;
import io.crest.constant.LogST;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 定义当前业务对象的核心职责
public @interface CrestAudit {
    String id() default "";

    String pid() default "";

    LogST st() default LogST.PANEL;

    LogOT ot();

    String stExp() default "";
}
