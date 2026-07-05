package io.crest.security;

import io.crest.constant.AuthConstant;
import io.crest.observability.ObservabilityProperties;
import io.crest.observability.PrometheusMetricsAccessFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusMetricsAccessFilterTest {

    @Test
    @DisplayName("Prometheus 未开启时指标端点应返回 404")
    void prometheusEndpointShouldBeHiddenWhenDisabled() throws IOException, ServletException {
        PrometheusMetricsAccessFilter filter = new PrometheusMetricsAccessFilter(defaultProperties(false, "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/actuator/prometheus");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(404, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("Prometheus 开启但未配置 token 时应失败关闭")
    void prometheusEndpointShouldFailClosedWhenTokenMissing() throws IOException, ServletException {
        PrometheusMetricsAccessFilter filter = new PrometheusMetricsAccessFilter(defaultProperties(true, ""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/actuator/prometheus");
        request.addHeader("Authorization", "Bearer any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("Prometheus token 错误时应拒绝访问")
    void prometheusEndpointShouldRejectInvalidToken() throws IOException, ServletException {
        PrometheusMetricsAccessFilter filter = new PrometheusMetricsAccessFilter(defaultProperties(true, "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/actuator/prometheus");
        request.addHeader("Authorization", "Bearer wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer realm=\"crest-prometheus\"", response.getHeader("WWW-Authenticate"));
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("Prometheus token 正确时应标记专用认证并放行")
    void prometheusEndpointShouldPassWithValidToken() throws IOException, ServletException {
        PrometheusMetricsAccessFilter filter = new PrometheusMetricsAccessFilter(defaultProperties(true, "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/actuator/prometheus");
        request.addHeader("Authorization", "Bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertSame(request, chain.getRequest());
        assertEquals(Boolean.TRUE, request.getAttribute(AuthConstant.PROMETHEUS_AUTHENTICATED_ATTR));
    }

    @Test
    @DisplayName("带 context-path 的指标端点应能正确匹配")
    void prometheusEndpointShouldMatchContextPath() throws IOException, ServletException {
        PrometheusMetricsAccessFilter filter = new PrometheusMetricsAccessFilter(defaultProperties(true, "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/crest/api/v1/actuator/prometheus");
        request.setContextPath("/crest");
        request.addHeader("Authorization", "Bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertSame(request, chain.getRequest());
    }

    private ObservabilityProperties defaultProperties(boolean enabled, String token) {
        ObservabilityProperties properties = new ObservabilityProperties();
        properties.getPrometheus().setEnabled(enabled);
        properties.getPrometheus().setEndpoint("/api/v1/actuator/prometheus");
        properties.getPrometheus().setToken(token);
        return properties;
    }
}
