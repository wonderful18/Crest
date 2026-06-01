package io.crest.security;

import io.crest.filter.HtmlResourceFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

class HtmlResourceFilterSecurityTest {

    @Test
    @DisplayName("HTML 资源过滤器不应弱化全局安全头")
    void shouldKeepSecurityHeadersConsistentWithGlobalFilter() throws ServletException, IOException {
        HtmlResourceFilter filter = new HtmlResourceFilter();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/");
        when(request.getContextPath()).thenReturn("");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader(eq("Content-Security-Policy"), contains("frame-ancestors 'self'"));
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(response).setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()");
        verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        verify(response, never()).setHeader(eq("Cache"), anyString());
        verify(chain).doFilter(request, response);
    }
}
