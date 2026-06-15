package io.crest.listener;


import io.crest.utils.ConfigUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

// 定义当前业务对象的核心职责
public class EhCacheStartListener implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    // 执行当前服务方法并返回处理结果
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String property = applicationContext.getEnvironment().getProperty("crest.login_timeout", String.class, "480");
        System.setProperty("crest.login_timeout", property);

        String ehcache = applicationContext.getEnvironment()
                .getProperty("crest.path.ehcache", String.class, ConfigUtils.getConfig("crest.path.ehcache", "/opt/crest/cache"));
        System.setProperty("crest.path.ehcache", ehcache);
    }
}
