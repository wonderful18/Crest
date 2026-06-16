package io.crest.config;


import io.crest.commons.utils.MybatisInterceptorConfig;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.interceptor.MybatisInterceptor;
import io.crest.interceptor.ObOracleReservedIdentifierInterceptor;
import io.crest.metadata.MetadataDbTypeResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableTransactionManagement
// 定义当前功能的配置入口
public class MybatisConfig {

    @Bean
    @ConditionalOnMissingBean
    // 执行当前服务方法并返回处理结果
    public MybatisInterceptor dbInterceptor(Environment environment) {
        MybatisInterceptor interceptor = new MybatisInterceptor();
        List<MybatisInterceptorConfig> configList = new ArrayList<>();
        configList.add(new MybatisInterceptorConfig(CoreEngine.class, "configuration"));
        configList.add(new MybatisInterceptorConfig(CoreDatasource.class, "configuration"));
        interceptor.setInterceptorConfigList(configList);
        interceptor.setUseAssignedKeysWithoutJdbcGeneratedKeys(
                MetadataDbTypeResolver.resolve(environment).isOracleCompatible());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public ObOracleReservedIdentifierInterceptor obOracleReservedIdentifierInterceptor(Environment environment) {
        return new ObOracleReservedIdentifierInterceptor(
                MetadataDbTypeResolver.resolve(environment).isOracleCompatible());
    }
}
