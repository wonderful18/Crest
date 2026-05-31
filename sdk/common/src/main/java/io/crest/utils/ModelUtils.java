package io.crest.utils;

import io.crest.model.CrestDeploymentMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelUtils {

    private static String modelValue;

    @Value("${spring.profiles.active:standalone}")
    public void setModelValue(String modelValue) {
        ModelUtils.modelValue = modelValue;
    }

    public static CrestDeploymentMode get() {
        return CrestDeploymentMode.valueOf(modelValue.toUpperCase());
    }

    public static boolean isDesktop() {
        return get() == CrestDeploymentMode.DESKTOP;
    }
}
