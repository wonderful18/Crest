package io.crest.datasource.provider;

import io.crest.utils.CommonBeanFactory;
import io.micrometer.common.util.StringUtils;

// 按抽取引擎类型查找对应 SQL provider
public class ProviderUtil {


    // 未配置引擎类型时仍按历史默认值使用 MySQL 抽取引擎
    public static EngineProvider getEngineProvider(String datasourceType) {
        if (StringUtils.isNotEmpty(datasourceType)) {
            return (EngineProvider) CommonBeanFactory.getBean(datasourceType + "Engine");
        } else {
            return CommonBeanFactory.getBean(MysqlEngineProvider.class);
        }
    }

}
