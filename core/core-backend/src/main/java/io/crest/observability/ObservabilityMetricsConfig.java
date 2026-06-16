package io.crest.observability;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 定义当前业务对象的核心职责
public class ObservabilityMetricsConfig {

    @Bean
    // 执行当前服务方法并返回处理结果
    public MeterFilter httpServerUriTagLimit(ObservabilityProperties observabilityProperties) {
        int maxUriTags = observabilityProperties.getPrometheus().getMaxUriTags();
        if (maxUriTags <= 0) {
            return MeterFilter.accept();
        }
        return MeterFilter.maximumAllowableTags("http.server.requests", "uri", maxUriTags, MeterFilter.deny());
    }
}
