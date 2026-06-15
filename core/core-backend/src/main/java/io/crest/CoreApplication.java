package io.crest;

import io.crest.listener.EhCacheStartListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
// 定义当前业务对象的核心职责
public class CoreApplication {

    // 执行当前服务方法并返回处理结果
    public static void main(String[] args) {
        SpringApplication context = new SpringApplication(CoreApplication.class);
        context.addInitializers(new EhCacheStartListener());
        context.run(args);
    }
}
