package io.crest.config;

import io.crest.constant.AuthConstant;
import io.crest.share.interceptor.LinkInterceptor;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static io.crest.constant.StaticResourceConstants.*;
import static io.crest.utils.StaticResourceUtils.ensureBoth;
import static io.crest.utils.StaticResourceUtils.ensureSuffix;
@Configuration
public class CrestMvcConfig implements WebMvcConfigurer {

    @Resource
    private LinkInterceptor linkInterceptor;

    @Value("${crest.swagger-ui.version:5.32.6}")
    private String swaggerUiVersion;

    @Value("${crest.path.static-resource:}")
    private String staticResourceDir;

    @Value("${crest.path.i18n.front:}")
    private String i18nFrontDir;

    /**
     * Configuring static resource path
     *
     * @param registry registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String workDir = fileLocation(StringUtils.defaultIfBlank(staticResourceDir, WORK_DIR));
        String uploadUrlPattern = ensureBoth(URL_SEPARATOR + UPLOAD_URL_PREFIX, AuthConstant.CREST_API_PREFIX, URL_SEPARATOR) + "**";
        registry.addResourceHandler(uploadUrlPattern).addResourceLocations(workDir);

        String i18nDir = fileLocation(StringUtils.defaultIfBlank(i18nFrontDir, I18N_DIR));
        String i18nUrlPattern = ensureBoth(I18N_URL, AuthConstant.CREST_API_PREFIX, URL_SEPARATOR) + "**";
        registry.addResourceHandler(i18nUrlPattern).addResourceLocations(i18nDir);

        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/" + swaggerUiVersion + "/");

    }

    private String fileLocation(String path) {
        if (path.startsWith(FILE_PROTOCOL)) {
            path = path.substring(FILE_PROTOCOL.length());
        }
        return FILE_PROTOCOL + ensureSuffix(path, FILE_SEPARATOR);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(linkInterceptor).addPathPatterns("/**");
    }
}
