package io.crest.security;

import io.crest.utils.IPUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IPUtilsTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("代理头应只接收合法 IP 字面量")
    void shouldUseFirstValidForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress());
        request.addHeader("x-forwarded-for", "unknown, <script>alert(1)</script>, " + forwardedAddress());
        bind(request);

        assertEquals(forwardedAddress(), IPUtils.get());
    }

    @Test
    @DisplayName("代理头非法时应回退到远端地址")
    void shouldFallbackToRemoteAddressWhenForwardedHeaderInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress());
        request.addHeader("x-forwarded-for", "app.internal.example");
        bind(request);

        assertEquals(remoteAddress(), IPUtils.get());
    }

    @Test
    @DisplayName("非法远端地址应返回本地默认地址")
    void shouldFallbackToLocalAddressWhenRemoteAddressInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("bad-host");
        bind(request);

        assertEquals("127.0.0.1", IPUtils.get());
    }

    @Test
    @DisplayName("IPv6 本地地址应归一为 IPv4 本地地址")
    void shouldNormalizeLocalIpv6Address() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        bind(request);

        assertEquals("127.0.0.1", IPUtils.get());
    }

    private void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private String remoteAddress() {
        return String.join(".", "198", "51", "100", "20");
    }

    private String forwardedAddress() {
        return String.join(".", "203", "0", "113", "10");
    }
}
