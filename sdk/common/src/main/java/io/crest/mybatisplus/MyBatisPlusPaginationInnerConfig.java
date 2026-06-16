package io.crest.mybatisplus;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import io.crest.metadata.MetadataDatabaseIdProvider;
import io.crest.metadata.MetadataDbType;
import io.crest.metadata.MetadataDbTypeResolver;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration

@EnableCaching
// 定义当前业务对象的核心职责
public class MyBatisPlusPaginationInnerConfig {

    private final Environment environment;

    public MyBatisPlusPaginationInnerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    // 执行当前服务方法并返回处理结果
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MetadataDbType metadataDbType = MetadataDbTypeResolver.resolve(environment);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(metadataDbType.getMybatisPlusDbType()));
        return interceptor;
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        return new MetadataDatabaseIdProvider(environment);
    }

}
