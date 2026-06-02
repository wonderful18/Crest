package io.crest.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ApiRouteInventoryMetrics implements SmartInitializingSingleton {

    private static final String ROUTE_INFO_METRIC = "crest.api.route.info";

    private final MeterRegistry meterRegistry;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ObservabilityProperties observabilityProperties;

    public ApiRouteInventoryMetrics(MeterRegistry meterRegistry,
                                    RequestMappingHandlerMapping requestMappingHandlerMapping,
                                    ObservabilityProperties observabilityProperties) {
        this.meterRegistry = meterRegistry;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.observabilityProperties = observabilityProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ObservabilityProperties.Api api = observabilityProperties.getApi();
        if (api == null || !api.isRouteInventoryEnabled()) {
            return;
        }

        List<RouteMetric> routes = collectRoutes(api.getRoutePrefix());
        for (RouteMetric route : routes) {
            Gauge.builder(ROUTE_INFO_METRIC, route, ignored -> 1)
                    .description("Registered Crest API routes. Value is always 1; labels describe the route.")
                    .tag("method", route.method())
                    .tag("uri", route.uri())
                    .tag("controller", route.controller())
                    .tag("handler", route.handler())
                    .strongReference(true)
                    .register(meterRegistry);
        }
    }

    private List<RouteMetric> collectRoutes(String routePrefix) {
        Set<RouteMetric> result = new LinkedHashSet<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((info, handlerMethod) -> {
            for (String pattern : routePatterns(info)) {
                String uri = normalizeUri(routePrefix, pattern);
                if (!isApiRoute(uri)) {
                    continue;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    result.add(toMetric("ANY", uri, handlerMethod));
                    continue;
                }
                methods.forEach(method -> result.add(toMetric(method.name(), uri, handlerMethod)));
            }
        });

        return result.stream()
                .sorted(Comparator.comparing(RouteMetric::uri).thenComparing(RouteMetric::method))
                .toList();
    }

    private Set<String> routePatterns(RequestMappingInfo info) {
        Set<String> patterns = new LinkedHashSet<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatternValues().forEach(patterns::add);
        }
        if (info.getPatternsCondition() != null) {
            patterns.addAll(info.getPatternsCondition().getPatterns());
        }
        return patterns;
    }

    private RouteMetric toMetric(String method, String uri, HandlerMethod handlerMethod) {
        String controller = handlerMethod.getBeanType().getSimpleName();
        String handler = controller + "." + handlerMethod.getMethod().getName();
        return new RouteMetric(method, uri, controller, handler);
    }

    private String normalizeUri(String routePrefix, String pattern) {
        String path = normalizePath(pattern);
        String prefix = normalizePath(routePrefix);
        if (StringUtils.isBlank(prefix) || "/".equals(prefix) || path.startsWith(prefix + "/")) {
            return path;
        }
        return prefix + path;
    }

    private String normalizePath(String value) {
        if (StringUtils.isBlank(value)) {
            return "/";
        }
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/{2,}", "/");
    }

    private boolean isApiRoute(String uri) {
        return uri.startsWith("/api/v1/")
                && !uri.startsWith("/api/v1/actuator/")
                && !uri.startsWith("/api/v1/error")
                && !uri.startsWith("/api/v1/v3/api-docs")
                && !uri.startsWith("/api/v1/swagger-ui")
                && !uri.startsWith("/api/v1/doc.html");
    }

    private record RouteMetric(String method, String uri, String controller, String handler) {
    }
}
