package io.crest.security;

import io.crest.config.SecurityHeadersConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("应设置 X-Frame-Options 头")
    void shouldSetXFrameOptions() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Frame-Options", "DENY");
    }

    @Test
    @DisplayName("应设置 X-Content-Type-Options 头")
    void shouldSetXContentTypeOptions() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Content-Type-Options", "nosniff");
    }

    @Test
    @DisplayName("应设置 X-XSS-Protection 头")
    void shouldSetXXssProtection() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
    }

    @Test
    @DisplayName("应设置 Content-Security-Policy 头")
    void shouldSetCSP() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("Content-Security-Policy"), contains("default-src 'self'"));
        verify(response).setHeader(eq("Content-Security-Policy"), contains("worker-src 'self' blob:"));
        verify(response).setHeader(eq("Content-Security-Policy"),
                argThat(value -> value != null && !value.contains("unsafe-inline") && !value.contains("unsafe-eval")));
    }

    @Test
    @DisplayName("应设置 Referrer-Policy 头")
    void shouldSetReferrerPolicy() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("HTTPS 请求应设置 Strict-Transport-Security 头")
    void shouldSetHSTSForHttps() throws ServletException, IOException {
        when(request.isSecure()).thenReturn(true);

        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("Strict-Transport-Security"), contains("max-age="));
    }

    @Test
    @DisplayName("外部网关为 HTTPS 时应设置 Strict-Transport-Security 头")
    void shouldSetHSTSForForwardedHttps() throws ServletException, IOException {
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("Strict-Transport-Security"), contains("max-age="));
    }

    @Test
    @DisplayName("HTTP 请求不应设置 Strict-Transport-Security 头")
    void shouldNotSetHSTSForHttp() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response, never()).setHeader(eq("Strict-Transport-Security"), anyString());
    }

    @Test
    @DisplayName("应设置缓存控制头")
    void shouldSetCacheControl() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
    }

    @Test
    @DisplayName("应移除服务器信息")
    void shouldRemoveServerInfo() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("Server", "");
        verify(response).setHeader("X-Powered-By", "");
    }

    @Test
    @DisplayName("应继续执行过滤器链")
    void shouldContinueFilterChain() throws ServletException, IOException {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
