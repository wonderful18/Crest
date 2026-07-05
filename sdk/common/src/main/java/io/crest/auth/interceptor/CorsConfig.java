package io.crest.auth.interceptor;

import io.crest.constant.AuthConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.stream.Stream;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${crest.cors-strict:true}")
    private boolean corsStrict;


    @Value("#{'${crest.origin-list:}'.split(',')}")
    private List<String> originList;

    private CorsRegistration operateCorsRegistration;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(AuthConstant.CREST_API_PREFIX, c -> c.isAnnotationPresent(RestController.class) && c.getPackageName().startsWith("io.crest"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        operateCorsRegistration = registry.addMapping("/**")
                .allowCredentials(false)
                .allowedHeaders("*")
                .maxAge(3600)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        String[] origins = normalizedOrigins(originList);
        if (origins.length > 0) {
            operateCorsRegistration.allowedOrigins(origins);
        }
    }

    public void addAllowedOrigins(List<String> origins) {
        if (!corsStrict || CollectionUtils.isEmpty(origins)) {
            return;
        }
        List<String> newOrigins = Stream.concat(origins.stream(), originList.stream())
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
        String[] originArray = newOrigins.toArray(new String[0]);
        if (originArray.length > 0) {
            operateCorsRegistration.allowedOrigins(originArray);
        }
    }

    String[] normalizedOrigins(List<String> origins) {
        if (CollectionUtils.isEmpty(origins)) {
            return new String[0];
        }
        return origins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toArray(String[]::new);
    }
}
