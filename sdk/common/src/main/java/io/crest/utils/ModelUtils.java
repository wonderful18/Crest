package io.crest.utils;

import io.crest.model.CrestDeploymentMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
// 提供当前模块复用的工具能力
public class ModelUtils {

    private static String modelValue;

    @Value("${spring.profiles.active:standalone}")
    // 执行当前服务方法并返回处理结果
    public void setModelValue(String modelValue) {
        ModelUtils.modelValue = modelValue;
    }

    // 执行当前服务方法并返回处理结果
    public static CrestDeploymentMode get() {
        return CrestDeploymentMode.valueOf(modelValue.toUpperCase());
    }

    // 判断当前类型是否满足业务分类
    public static boolean isDesktop() {
        return get() == CrestDeploymentMode.DESKTOP;
    }
}
