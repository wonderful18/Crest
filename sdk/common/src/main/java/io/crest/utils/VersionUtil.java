package io.crest.utils;

import org.springframework.core.env.Environment;

public class VersionUtil {


    public static String getRandomVersion() {
        Environment environment = CommonBeanFactory.getBean(Environment.class);
        return environment == null ? "1.5.0" : environment.getProperty("crest.version", "1.5.0");
    }

}
