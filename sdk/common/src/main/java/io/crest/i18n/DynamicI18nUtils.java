package io.crest.i18n;

import jakarta.annotation.Resource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

@Component
// 定义当前业务对象的核心职责
public class DynamicI18nUtils {


    private static ReloadableResourceBundleMessageSource messageSource;

    @Resource
    // 执行当前服务方法并返回处理结果
    public void setMessageSource(ReloadableResourceBundleMessageSource messageSource) {
        DynamicI18nUtils.messageSource = messageSource;
    }

    // 格式化日期时间并返回统一展示值
    public static void addOrUpdate(String baseName) {
        messageSource.addBasenames(baseName);
        messageSource.setCacheSeconds(0);
        messageSource.clearCache();
    }
}
