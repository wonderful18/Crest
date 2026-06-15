package io.crest.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;

// 提供当前模块复用的工具能力
public class CrestReflectionUtils {

    // 执行当前服务方法并返回处理结果
    public static Method findMethod(Class<?> cla, String methodName) {
        Method[] methods = cla.getMethods();
        if (ArrayUtils.isEmpty(methods)) return null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)){
                return method;
            }
        }
        return null;
    }
}
