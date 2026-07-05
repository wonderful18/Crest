package io.crest.security;

import io.crest.auth.filter.TokenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenFilterMethodTest {

    @Test
    @DisplayName("PUT 请求不应被鉴权过滤器直接拒绝")
    void putRequestsShouldPassMethodValidation() throws IOException, ServletException {
        TokenFilter filter = new TokenFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/v1/public-key");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("未支持的请求方法仍应被拒绝")
    void unsupportedMethodsShouldBeRejected() throws IOException, ServletException {
        TokenFilter filter = new TokenFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("PATCH");

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(chain, never()).doFilter(request, response);
    }
}
