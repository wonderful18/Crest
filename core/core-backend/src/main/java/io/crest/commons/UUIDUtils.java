package io.crest.commons;

import java.util.UUID;

// 定义当前业务对象的核心职责
public class UUIDUtils {

    // 执行当前服务方法并返回处理结果
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

}
