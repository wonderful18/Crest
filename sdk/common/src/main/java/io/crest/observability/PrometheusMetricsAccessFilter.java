package io.crest.observability;

import io.crest.constant.AuthConstant;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrometheusMetricsAccessFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusMetricsAccessFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObservabilityProperties observabilityProperties;

    private final AtomicBoolean tokenMissingLogged = new AtomicBoolean(false);

    public PrometheusMetricsAccessFilter(ObservabilityProperties observabilityProperties) {
        this.observabilityProperties = observabilityProperties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (!isPrometheusRequest(request)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        ObservabilityProperties.Prometheus prometheus = observabilityProperties.getPrometheus();
        if (!prometheus.isEnabled()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String expectedToken = StringUtils.trimToNull(prometheus.getToken());
        if (expectedToken == null) {
            if (tokenMissingLogged.compareAndSet(false, true)) {
                LOGGER.info("Prometheus metrics endpoint is enabled but CREST_PROMETHEUS_TOKEN is not configured");
            }
            writeJson(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Prometheus token is not configured");
            return;
        }

        String presentedToken = extractBearerToken(request.getHeader("Authorization"));
        if (!constantTimeEquals(expectedToken, presentedToken)) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"crest-prometheus\"");
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        request.setAttribute(AuthConstant.PROMETHEUS_AUTHENTICATED_ATTR, Boolean.TRUE);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isPrometheusRequest(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        String contextPath = normalizePath(request.getContextPath());
        if (StringUtils.isNotBlank(contextPath) && path.startsWith(contextPath + "/")) {
            path = path.substring(contextPath.length());
        }
        String configuredEndpoint = normalizePath(observabilityProperties.getPrometheus().getEndpoint());
        return List.of(configuredEndpoint, "/api/v1/actuator/prometheus", "/actuator/prometheus").stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(path::equals);
    }

    private String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return StringUtils.trimToNull(authorization.substring(BEARER_PREFIX.length()));
    }

    private boolean constantTimeEquals(String expectedToken, String presentedToken) {
        if (StringUtils.isBlank(expectedToken) || StringUtils.isBlank(presentedToken)) {
            return false;
        }
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] presented = presentedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, presented);
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + message + "\"}");
    }
}
