package io.crest.datasource.provider;

import io.crest.utils.CommonBeanFactory;
import io.micrometer.common.util.StringUtils;

// 定义当前业务对象的核心职责
public class ProviderUtil {


    // 执行当前服务方法并返回处理结果
    public static EngineProvider getEngineProvider(String datasourceType) {
        if (StringUtils.isNotEmpty(datasourceType)) {
            return (EngineProvider) CommonBeanFactory.getBean(datasourceType + "Engine");
        } else {
            return CommonBeanFactory.getBean(MysqlEngineProvider.class);
        }
    }

}
